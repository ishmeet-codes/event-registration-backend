--liquibase formatted sql
--changeset ishmeet:V004_create_role_permissions_table

------------------------------------------------------------
--Roles & Permissions
------------------------------------------------------------
CREATE TABLE role_permissions (
     id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id BIGINT,
    permission_id BIGINT,
    CONSTRAINT fk_roles FOREIGN KEY(role_id) REFERENCES roles(id),
    CONSTRAINT fk_roles_permissions FOREIGN KEY(permission_id) REFERENCES permissions(id)
);