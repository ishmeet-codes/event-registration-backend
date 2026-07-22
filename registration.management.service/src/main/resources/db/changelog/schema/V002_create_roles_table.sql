--liquibase formatted sql

--changeset ishmeet:V002_create_roles_table

------------------------------------------------------------
-- Roles
------------------------------------------------------------
CREATE TABLE roles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_code VARCHAR(50) UNIQUE,
    role_name VARCHAR(50) UNIQUE,
    description TEXT,
    system_role BOOLEAN,
    active BOOLEAN,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);