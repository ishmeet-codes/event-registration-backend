--liquibase formatted sql

--changeset ishmeet:V011_create_registrations_table

------------------------------------------------------------
-- Registrations
------------------------------------------------------------
CREATE TABLE  registrations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id BIGINT,
    event_id BIGINT,
    created_by_staff_id BIGINT,
    status registration_status NOT NULL,
    remarks TEXT NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_registration_school_event UNIQUE (school_id, event_id),
    CONSTRAINT fk_registrations_school_id FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_registrations_event_id FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_registrations_created_by_staff_id FOREIGN KEY (created_by_staff_id) REFERENCES school_staff(id),
    CONSTRAINT fk_registration_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_registration_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);