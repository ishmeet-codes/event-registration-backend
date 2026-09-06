--liquibase formatted sql

--changeset agent:V023_checkin_management_schema

------------------------------------------------------------
-- Redesign checkins table for complete Check-In Management
------------------------------------------------------------
DROP TABLE IF EXISTS checkins CASCADE;

CREATE TABLE checkins (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    participant_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CHECKED_IN',
    checked_in_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    checked_out_at TIMESTAMP,
    checked_in_by BIGINT NOT NULL,
    checked_out_by BIGINT,
    remarks VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_checkins_participant UNIQUE (participant_id),
    CONSTRAINT fk_checkins_participant FOREIGN KEY (participant_id) REFERENCES participants(id) ON DELETE CASCADE,
    CONSTRAINT fk_checkins_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_checkins_registration FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_checkins_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_checkins_checked_in_by FOREIGN KEY (checked_in_by) REFERENCES users(id),
    CONSTRAINT fk_checkins_checked_out_by FOREIGN KEY (checked_out_by) REFERENCES users(id)
);

CREATE INDEX idx_checkins_participant_id ON checkins(participant_id);
CREATE INDEX idx_checkins_event_id ON checkins(event_id);
CREATE INDEX idx_checkins_registration_id ON checkins(registration_id);
CREATE INDEX idx_checkins_school_id ON checkins(school_id);
CREATE INDEX idx_checkins_status ON checkins(status);
