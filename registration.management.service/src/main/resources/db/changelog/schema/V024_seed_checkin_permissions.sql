--liquibase formatted sql

--changeset agent:V024_seed_checkin_permissions

-- Ensure CHECKIN_TEAM role exists
INSERT INTO roles (role_code, role_name, description, system_role, active)
VALUES
    ('CHECKIN_TEAM', 'Check-in Team', 'Operational user responsible for participant check-in and check-out management at events.', TRUE, TRUE)
ON CONFLICT (role_code) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    active = EXCLUDED.active;

-- Insert Check-in permissions
INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('CHECKIN_VIEW', 'View Check-ins', 'View participant check-in records and attendance status'),
    ('CHECKIN_CREATE', 'Create Check-in', 'Perform participant check-in'),
    ('CHECKIN_UPDATE', 'Update Check-in', 'Update or correct participant check-in record / perform check-out'),
    ('CHECKIN_DELETE', 'Delete Check-in', 'Delete participant check-in record'),
    ('CHECKIN_BULK', 'Bulk Check-in', 'Perform bulk check-in for participants'),
    ('CHECKIN_VIEW_REPORTS', 'View Check-in Reports', 'View attendance summaries and reports')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    description = EXCLUDED.description;

-- Grant FULL Check-in permissions to Organizing & Check-in Teams (SUPER_ADMIN, ADMIN, EVENT_MANAGER, CHECKIN_TEAM)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'CHECKIN_VIEW', 'CHECKIN_CREATE', 'CHECKIN_UPDATE', 'CHECKIN_DELETE', 'CHECKIN_BULK', 'CHECKIN_VIEW_REPORTS'
)
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN', 'EVENT_MANAGER', 'CHECKIN_TEAM')
ON CONFLICT DO NOTHING;

-- Grant READ-ONLY Check-in status view permissions to Teachers, School Staff, Viewers & Participants
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code = 'CHECKIN_VIEW'
WHERE r.role_code IN ('LOGIN_TEACHER', 'ACCOMPANYING_TEACHER', 'SCHOOL_STAFF', 'VIEWER', 'PARTICIPANT')
ON CONFLICT DO NOTHING;
