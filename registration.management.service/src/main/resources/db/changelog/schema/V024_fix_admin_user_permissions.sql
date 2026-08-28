--liquibase formatted sql

--changeset ishmeet:V024_fix_admin_user_permissions
-- Ensure ADMIN role has all user-management permissions added in V022
-- (USER_VIEW, ROLE_ASSIGN, ROLE_VIEW were added after V018 ran)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN (
             'USER_VIEW',
             'USER_CREATE',
             'USER_UPDATE',
             'USER_DELETE',
             'USER_READ',
             'ROLE_VIEW',
             'ROLE_READ',
             'ROLE_ASSIGN',
             'PERMISSION_VIEW',
             'PERMISSION_CREATE',
             'PERMISSION_UPDATE',
             'PERMISSION_DELETE'
         )
WHERE r.role_code = 'ADMIN'
    ON CONFLICT DO NOTHING;

-- Ensure SUPER_ADMIN picks up any permissions added after V021
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.role_code = 'SUPER_ADMIN'
    ON CONFLICT DO NOTHING;
