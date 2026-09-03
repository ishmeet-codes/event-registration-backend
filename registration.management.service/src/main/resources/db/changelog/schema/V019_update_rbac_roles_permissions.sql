--liquibase formatted sql

--changeset ishmeet:V019_update_rbac_roles_permissions

INSERT INTO roles (role_code, role_name, description, system_role, active)
VALUES
    ('LOGIN_TEACHER', 'Login Teacher', 'Lead school teacher responsible for creating and managing school registrations and staff.', TRUE, TRUE),
    ('ACCOMPANYING_TEACHER', 'Accompanying Teacher', 'Accompanying teacher with access to view school registrations and participants.', TRUE, TRUE)
ON CONFLICT (role_code) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    active = EXCLUDED.active;

-- Permissions for LOGIN_TEACHER (Full operational control over their school & registrations)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'SCHOOL_VIEW', 'SCHOOL_CREATE', 'SCHOOL_UPDATE',
    'SCHOOL_STAFF_VIEW', 'SCHOOL_STAFF_CREATE', 'SCHOOL_STAFF_UPDATE', 'SCHOOL_STAFF_DELETE', 'SCHOOL_STAFF_STATUS_UPDATE',
    'EVENT_VIEW', 'REGISTRATION_VIEW', 'REGISTRATION_CREATE', 'REGISTRATION_UPDATE', 'REGISTRATION_DELETE', 'REGISTRATION_STATUS_UPDATE',
    'PARTICIPANT_VIEW', 'PARTICIPANT_CREATE', 'PARTICIPANT_UPDATE', 'PROFILE_UPDATE'
)
WHERE r.role_code = 'LOGIN_TEACHER'
ON CONFLICT DO NOTHING;

-- Permissions for ACCOMPANYING_TEACHER (Read-only access for assigned event operational tasks)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'SCHOOL_VIEW', 'SCHOOL_STAFF_VIEW', 'EVENT_VIEW', 'REGISTRATION_VIEW', 'PARTICIPANT_VIEW', 'PROFILE_UPDATE'
)
WHERE r.role_code = 'ACCOMPANYING_TEACHER'
ON CONFLICT DO NOTHING;
