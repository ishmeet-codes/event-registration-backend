--liquibase formatted sql

--changeset agent:V038_create_refreshment_tables

-- ==============================================================================
-- Migration V038: Create Refreshment Management Tables
-- ==============================================================================

-- 1. Create refreshment_plans table
CREATE TABLE IF NOT EXISTS refreshment_plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refreshment_plans_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_refreshment_plans_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- 2. Create refreshment_sessions table
CREATE TABLE IF NOT EXISTS refreshment_sessions (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    event_id BIGINT,
    recipient_category VARCHAR(50) NOT NULL,
    tracking_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    expected_count INT DEFAULT 0,
    count_distributed INT DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    display_order INT NOT NULL DEFAULT 1,
    require_checkin_presence BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refreshment_sessions_plan FOREIGN KEY (plan_id) REFERENCES refreshment_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_refreshment_sessions_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE SET NULL,
    CONSTRAINT fk_refreshment_sessions_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_refreshment_sessions_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- 3. Create refreshment_teams table
CREATE TABLE IF NOT EXISTS refreshment_teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    leader_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refreshment_teams_leader FOREIGN KEY (leader_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_refreshment_teams_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_refreshment_teams_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- 4. Create refreshment_team_members table
CREATE TABLE IF NOT EXISTS refreshment_team_members (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refreshment_team_members_team FOREIGN KEY (team_id) REFERENCES refreshment_teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_refreshment_team_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_refreshment_team_member UNIQUE (team_id, user_id)
);

-- 5. Create refreshment_assignments table
CREATE TABLE IF NOT EXISTS refreshment_assignments (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refreshment_assignments_session FOREIGN KEY (session_id) REFERENCES refreshment_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_refreshment_assignments_team FOREIGN KEY (team_id) REFERENCES refreshment_teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_refreshment_assignments_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uq_refreshment_session_team UNIQUE (session_id, team_id)
);

-- 6. Create guest_records table
CREATE TABLE IF NOT EXISTS guest_records (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    designation VARCHAR(255),
    organization VARCHAR(255),
    contact_number VARCHAR(50),
    remarks TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guest_records_plan FOREIGN KEY (plan_id) REFERENCES refreshment_plans(id) ON DELETE CASCADE
);

-- 7. Create refreshment_distributions table
CREATE TABLE IF NOT EXISTS refreshment_distributions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    recipient_category VARCHAR(50) NOT NULL,
    participant_id BIGINT,
    school_staff_id BIGINT,
    oc_member_id BIGINT,
    guest_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'GIVEN',
    distributed_by BIGINT NOT NULL,
    distributed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(500),
    correction_reason VARCHAR(500),
    corrected_by BIGINT,
    corrected_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ref_dist_session FOREIGN KEY (session_id) REFERENCES refreshment_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_ref_dist_participant FOREIGN KEY (participant_id) REFERENCES participants(id) ON DELETE SET NULL,
    CONSTRAINT fk_ref_dist_staff FOREIGN KEY (school_staff_id) REFERENCES school_staff(id) ON DELETE SET NULL,
    CONSTRAINT fk_ref_dist_oc FOREIGN KEY (oc_member_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_ref_dist_guest FOREIGN KEY (guest_id) REFERENCES guest_records(id) ON DELETE SET NULL,
    CONSTRAINT fk_ref_dist_distributed_by FOREIGN KEY (distributed_by) REFERENCES users(id),
    CONSTRAINT fk_ref_dist_corrected_by FOREIGN KEY (corrected_by) REFERENCES users(id)
);

-- 8. Create guest_count_distributions table
CREATE TABLE IF NOT EXISTS guest_count_distributions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    count_distributed INT NOT NULL,
    distributed_by BIGINT NOT NULL,
    distributed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guest_count_dist_session FOREIGN KEY (session_id) REFERENCES refreshment_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_guest_count_dist_by FOREIGN KEY (distributed_by) REFERENCES users(id)
);

-- 9. Create Performance Indexes
CREATE INDEX IF NOT EXISTS idx_ref_plans_date ON refreshment_plans(event_date, active);
CREATE INDEX IF NOT EXISTS idx_ref_sessions_plan_order ON refreshment_sessions(plan_id, display_order);
CREATE INDEX IF NOT EXISTS idx_ref_sessions_event ON refreshment_sessions(event_id);
CREATE INDEX IF NOT EXISTS idx_ref_sessions_category ON refreshment_sessions(recipient_category);
CREATE INDEX IF NOT EXISTS idx_ref_assignments_session ON refreshment_assignments(session_id);
CREATE INDEX IF NOT EXISTS idx_ref_assignments_team ON refreshment_assignments(team_id);
CREATE INDEX IF NOT EXISTS idx_ref_team_members_user ON refreshment_team_members(user_id);
CREATE INDEX IF NOT EXISTS idx_ref_dist_session_status ON refreshment_distributions(session_id, status);
CREATE INDEX IF NOT EXISTS idx_ref_dist_participant ON refreshment_distributions(participant_id);
CREATE INDEX IF NOT EXISTS idx_ref_dist_staff ON refreshment_distributions(school_staff_id);
CREATE INDEX IF NOT EXISTS idx_ref_dist_oc ON refreshment_distributions(oc_member_id);
CREATE INDEX IF NOT EXISTS idx_ref_dist_guest ON refreshment_distributions(guest_id);
