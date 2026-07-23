--liquibase formatted sql

--changeset ishmeet:V015_create_indexes

------------------------------------------------------------
-- USERS
------------------------------------------------------------
CREATE INDEX idx_users_role_id
    ON users(role_id);

CREATE INDEX idx_users_active
    ON users(active);

------------------------------------------------------------
-- REFRESH TOKENS
------------------------------------------------------------
CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_token
    ON refresh_tokens(token);

CREATE INDEX idx_refresh_tokens_expiry
    ON refresh_tokens(expires_at);

------------------------------------------------------------
-- SCHOOLS
------------------------------------------------------------
CREATE INDEX idx_schools_code
    ON schools(school_code);

CREATE INDEX idx_schools_city
    ON schools(city);

CREATE INDEX idx_schools_active
    ON schools(active);

------------------------------------------------------------
-- SCHOOL STAFF
------------------------------------------------------------
CREATE INDEX idx_school_staff_school_id
    ON school_staff(school_id);

CREATE INDEX idx_school_staff_user_id
    ON school_staff(user_id);

CREATE INDEX idx_school_staff_email
    ON school_staff(email);

CREATE INDEX idx_school_staff_phone
    ON school_staff(phone);

CREATE INDEX idx_school_staff_role
    ON school_staff(staff_role);

CREATE INDEX idx_school_staff_active
    ON school_staff(active);

------------------------------------------------------------
-- Only one LOGIN_TEACHER per school
------------------------------------------------------------
CREATE UNIQUE INDEX uq_school_login_teacher
    ON school_staff(school_id)
    WHERE staff_role = 'LOGIN_TEACHER';

------------------------------------------------------------
-- EVENTS
------------------------------------------------------------
CREATE INDEX idx_events_code
    ON events(id);

CREATE INDEX idx_events_category
    ON events(participation_category_code);

CREATE INDEX idx_events_active
    ON events(active);

------------------------------------------------------------
-- REGISTRATIONS
------------------------------------------------------------
CREATE INDEX idx_registrations_school
    ON registrations(school_id);

CREATE INDEX idx_registrations_event
    ON registrations(event_id);

CREATE INDEX idx_registrations_staff
    ON registrations(created_by_staff_id);

CREATE INDEX idx_registrations_status
    ON registrations(status);

CREATE INDEX idx_registrations_created_at
    ON registrations(created_at);

------------------------------------------------------------
-- PARTICIPANTS
------------------------------------------------------------
CREATE INDEX idx_participants_registration
    ON participants(registration_id);

CREATE INDEX idx_participants_name
    ON participants(full_name);

CREATE INDEX idx_participants_guardian_phone
    ON participants(gaurdian_phone);

CREATE INDEX idx_participants_created_by
    ON participants(created_by);

------------------------------------------------------------
-- CHECKINS
------------------------------------------------------------
CREATE INDEX idx_checkins_school
    ON checkins(school_id);

CREATE INDEX idx_checkins_checked_by
    ON checkins(checked_in_by);

CREATE INDEX idx_checkins_checked_at
    ON checkins(checked_in_at);

CREATE UNIQUE INDEX uq_checkins_participant
    ON checkins(participant_id)
    WHERE participant_id IS NOT NULL;

CREATE UNIQUE INDEX uq_checkins_school_staff
    ON checkins(school_staff_id)
    WHERE school_staff_id IS NOT NULL;

------------------------------------------------------------
-- AUDIT LOGS
------------------------------------------------------------
CREATE INDEX idx_audit_logs_user
    ON audit_logs(user_id);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs(entity_name, entity_id);

CREATE INDEX idx_audit_logs_action
    ON audit_logs(action);

CREATE INDEX idx_audit_logs_status
    ON audit_logs(status);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs(created_at);