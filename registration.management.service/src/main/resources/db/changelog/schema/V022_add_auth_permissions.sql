--liquibase formatted sql

--changeset ishmeet:V022_add_auth_permissions

INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('USER_VIEW', 'View Users', 'View users with pagination/filtering'),
    ('ROLE_ASSIGN', 'Assign Roles', 'Assign roles to users'),
    ('ROLE_VIEW', 'View Roles', 'View roles'),
    ('PERMISSION_CREATE', 'Create Permissions', 'Create permissions'),
    ('PERMISSION_VIEW', 'View Permissions', 'View permissions'),
    ('PERMISSION_UPDATE', 'Update Permissions', 'Update permissions'),
    ('PERMISSION_DELETE', 'Delete Permissions', 'Delete permissions')
    ON CONFLICT (permission_code) DO NOTHING;

-- Assign all permissions to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.role_code = 'SUPER_ADMIN'
    ON CONFLICT DO NOTHING;
