--liquibase formatted sql

--changeset agent:V026_seed_reporting_permissions

-- Insert Reporting & Analytics permissions
INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('REPORT_VIEW', 'View Basic Reports', 'Access to general overview, summary, and dashboard reports'),
    ('REPORT_VIEW_DETAILED', 'View Detailed Reports', 'Access to detailed registration, participant, and school breakdown reports'),
    ('REPORT_VIEW_ATTENDANCE', 'View Attendance Reports', 'Access to event and school attendance tracking reports'),
    ('REPORT_VIEW_ANALYTICS', 'View Analytics & Performance', 'Access to advanced analytics, event comparisons, and category insights'),
    ('REPORT_EXPORT', 'Export Reports', 'Export reports in CSV format')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    description = EXCLUDED.description;

-- SUPER_ADMIN, ADMIN, EVENT_MANAGER, CHECKIN_TEAM: Grant all reporting permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'REPORT_VIEW', 'REPORT_VIEW_DETAILED', 'REPORT_VIEW_ATTENDANCE', 'REPORT_VIEW_ANALYTICS', 'REPORT_EXPORT'
)
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN', 'EVENT_MANAGER', 'CHECKIN_TEAM')
ON CONFLICT DO NOTHING;

-- SCHOOL_STAFF, LOGIN_TEACHER, ACCOMPANYING_TEACHER: REPORT_VIEW, REPORT_VIEW_DETAILED, REPORT_VIEW_ATTENDANCE, REPORT_EXPORT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'REPORT_VIEW', 'REPORT_VIEW_DETAILED', 'REPORT_VIEW_ATTENDANCE', 'REPORT_EXPORT'
)
WHERE r.role_code IN ('SCHOOL_STAFF', 'LOGIN_TEACHER', 'ACCOMPANYING_TEACHER')
ON CONFLICT DO NOTHING;
