-- Liquibase Migration: V029_seed_qr_checkin_permissions.sql

INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('CHECKIN_CREDENTIAL_VIEW', 'View QR Credentials', 'View active QR check-in credentials'),
    ('CHECKIN_CREDENTIAL_REGENERATE', 'Regenerate QR Credentials', 'Regenerate QR check-in credentials'),
    ('CHECKIN_SCAN', 'Scan QR Check-in', 'Scan and process QR code check-ins')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    description = EXCLUDED.description;

-- Grant SCAN permission to admin & operational check-in team
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code = 'CHECKIN_SCAN'
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN', 'EVENT_MANAGER', 'CHECKIN_TEAM')
ON CONFLICT DO NOTHING;

-- Grant CREDENTIAL VIEW and REGENERATE to users and staff
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN ('CHECKIN_CREDENTIAL_VIEW', 'CHECKIN_CREDENTIAL_REGENERATE')
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN', 'EVENT_MANAGER', 'CHECKIN_TEAM', 'LOGIN_TEACHER', 'ACCOMPANYING_TEACHER', 'SCHOOL_STAFF', 'PARTICIPANT')
ON CONFLICT DO NOTHING;
