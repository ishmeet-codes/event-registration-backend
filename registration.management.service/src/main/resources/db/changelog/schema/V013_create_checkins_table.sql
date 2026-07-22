--liquibase formatted sql

--changeset ishmeet:V013_create_checkins_table

------------------------------------------------------------
-- Checkins
------------------------------------------------------------
CREATE TABLE checkins (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id BIGINT,
    participant_id BIGINT NOT NULL,
    school_staff_id BIGINT NOT NULL,
    present BOOLEAN,
    checked_in_by BIGINT NOT NULL,
    checked_in_at BIGINT NOT NULL,
    remarks TEXT,
    CONSTRAINT fk_checkins_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_checkins_participant FOREIGN KEY (participant_id) REFERENCES participants(id),
    CONSTRAINT fk_checkins_school_staff FOREIGN KEY (school_staff_id) REFERENCES school_staff(id),
    CONSTRAINT fk_checkins_checked_by FOREIGN KEY (checked_in_by) REFERENCES users(id),
    CONSTRAINT chk_checkin_target CHECK (
            (participant_id IS NOT NULL AND school_staff_id IS NULL)
                OR
            (participant_id IS NULL AND school_staff_id IS NOT NULL)
            )
);
