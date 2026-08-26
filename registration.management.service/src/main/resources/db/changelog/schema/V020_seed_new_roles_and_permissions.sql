--liquibase formatted sql

--changeset ishmeet:V020_seed_new_roles

INSERT INTO roles (role_code, role_name, description, system_role, active)
VALUES
    ('EVENT_ORGANIZER', 'Event Organizer', 'Can create and manage events and registrations', TRUE, TRUE),
    ('ATTENDEE',        'Attendee',        'Can view events and manage own registrations',   TRUE, TRUE),
    ('GUEST',           'Guest',           'Read-only access to public event information',   TRUE, TRUE)
    ON CONFLICT (role_code) DO NOTHING;

--changeset ishmeet:V020_seed_read_write_delete_permissions

INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('READ',   'Read Access',   'Grants read/view access to resources'),
    ('WRITE',  'Write Access',  'Grants create and update access to resources'),
    ('DELETE', 'Delete Access', 'Grants delete access to resources')
    ON CONFLICT (permission_code) DO NOTHING;

--changeset ishmeet:V020_seed_role_permissions_new_roles

-- EVENT_ORGANIZER -> READ + WRITE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN ('READ', 'WRITE')
WHERE r.role_code = 'EVENT_ORGANIZER'
    ON CONFLICT DO NOTHING;

-- ATTENDEE -> READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN ('READ')
WHERE r.role_code = 'ATTENDEE'
    ON CONFLICT DO NOTHING;

-- GUEST -> READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN ('READ')
WHERE r.role_code = 'GUEST'
    ON CONFLICT DO NOTHING;

--changeset ishmeet:V020_seed_role_permissions_existing_roles_read_write_delete

-- SUPER_ADMIN already gets all via CROSS JOIN in V018; no extra needed
-- ADMIN -> also gets READ, WRITE, DELETE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN ('READ', 'WRITE', 'DELETE')
WHERE r.role_code = 'ADMIN'
    ON CONFLICT DO NOTHING;

-- REGISTRATION_TEAM -> READ + WRITE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN ('READ', 'WRITE')
WHERE r.role_code = 'REGISTRATION_TEAM'
    ON CONFLICT DO NOTHING;

-- CHECKIN_TEAM -> READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN ('READ')
WHERE r.role_code = 'CHECKIN_TEAM'
    ON CONFLICT DO NOTHING;

-- CERTIFICATE_TEAM -> READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN ('READ')
WHERE r.role_code = 'CERTIFICATE_TEAM'
    ON CONFLICT DO NOTHING;
