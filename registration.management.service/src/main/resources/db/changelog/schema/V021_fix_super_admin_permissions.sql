--liquibase formatted sql

--changeset ishmeet:V021_fix_super_admin_new_permissions

-- V018 seeded SUPER_ADMIN via CROSS JOIN, but READ/WRITE/DELETE did not exist then.
-- Re-run the CROSS JOIN so SUPER_ADMIN picks up the new permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.role_code = 'SUPER_ADMIN'
    ON CONFLICT DO NOTHING;
