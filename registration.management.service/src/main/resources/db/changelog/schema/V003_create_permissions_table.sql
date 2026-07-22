--liquibase formatted sql

--changeset ishmeet:V003_create_permissions_table

------------------------------------------------------------
--Permissions
------------------------------------------------------------
CREATE TABLE permissions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    permission_code VARCHAR(50) UNIQUE,
    permission_name VARCHAR(150),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);