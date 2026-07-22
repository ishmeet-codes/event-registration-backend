--liquibase formatted sql

--changeset ishmeet:V008_create_schools_table

------------------------------------------------------------
-- Schools
------------------------------------------------------------
CREATE TABLE schools (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_code VARCHAR(50) UNIQUE,
    school_name VARCHAR(150),
    principal_name VARCHAR(120),
    board VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    district VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    phone VARCHAR(20),
    email VARCHAR(120),
    active BOOlEAN,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_schools_01 FOREIGN KEY (created_by) REFERENCES roles(id),
    CONSTRAINT fk_schools_02 FOREIGN KEY (updated_by) REFERENCES roles(id)
);

