-- Liquibase Migration: V028_add_check_in_method_to_checkins.sql
ALTER TABLE checkins ALTER COLUMN participant_id DROP NOT NULL;

ALTER TABLE checkins ADD COLUMN IF NOT EXISTS check_in_method VARCHAR(20) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE checkins ADD COLUMN IF NOT EXISTS school_staff_id BIGINT NULL REFERENCES school_staff(id) ON DELETE CASCADE;

ALTER TABLE checkins ADD CONSTRAINT chk_checkin_person_owner CHECK (
    (participant_id IS NOT NULL AND school_staff_id IS NULL) OR
    (participant_id IS NULL AND school_staff_id IS NOT NULL)
);

CREATE INDEX idx_checkins_school_staff_id ON checkins(school_staff_id);
CREATE INDEX idx_checkins_check_in_method ON checkins(check_in_method);
CREATE UNIQUE INDEX uq_checkins_staff_event ON checkins(school_staff_id, event_id) WHERE school_staff_id IS NOT NULL;
