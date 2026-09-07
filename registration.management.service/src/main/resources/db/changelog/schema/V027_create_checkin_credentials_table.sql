-- Liquibase Migration: V027_create_checkin_credentials_table.sql
CREATE TABLE checkin_credentials (
    id BIGSERIAL PRIMARY KEY,
    credential_type VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    participant_id BIGINT NULL REFERENCES participants(id) ON DELETE CASCADE,
    school_staff_id BIGINT NULL REFERENCES school_staff(id) ON DELETE CASCADE,
    registration_id BIGINT NOT NULL REFERENCES registrations(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT chk_checkin_credential_owner CHECK (
        (participant_id IS NOT NULL AND school_staff_id IS NULL) OR
        (participant_id IS NULL AND school_staff_id IS NOT NULL)
    )
);

CREATE INDEX idx_checkin_credentials_token_hash ON checkin_credentials(token_hash);
CREATE INDEX idx_checkin_credentials_participant ON checkin_credentials(participant_id);
CREATE INDEX idx_checkin_credentials_staff ON checkin_credentials(school_staff_id);
CREATE INDEX idx_checkin_credentials_registration ON checkin_credentials(registration_id);
CREATE INDEX idx_checkin_credentials_event ON checkin_credentials(event_id);
CREATE INDEX idx_checkin_credentials_active ON checkin_credentials(active);
