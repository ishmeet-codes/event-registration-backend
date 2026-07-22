--liquibase formatted sql

--changeset ishmeet:V009_create_school_staff_table

------------------------------------------------------------
-- School Staff
------------------------------------------------------------
CREATE TABLE school_staff (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id BIGINT NOT NULL,
    user_id BIGINT,
    full_name VARCHAR(120) NOT NULL,
    designation VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(120) NOT NULL,
    staff_role staff_role NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_school_staff_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_school_staff_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_school_staff_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_school_staff_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);