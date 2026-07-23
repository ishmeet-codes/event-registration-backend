--liquibase formatted sql

--changeset ishmeet:V012_create_participants_table.sql

------------------------------------------------------------
-- Participants
------------------------------------------------------------
CREATE TABLE participants (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    gender gender NOT NULL,
    class_name VARCHAR(20),
    dob DATE NOT NULL,
    guardian_phone VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_participants_registration_id FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT fk_participants_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_participants_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);