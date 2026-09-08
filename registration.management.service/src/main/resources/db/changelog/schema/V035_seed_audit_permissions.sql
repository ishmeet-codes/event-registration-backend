--liquibase formatted sql

--changeset agent:V035_seed_audit_permissions

-- Insert Audit Management permissions
INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('AUDIT_LOG_VIEW', 'View Audit Logs', 'View, filter, and inspect detailed system audit trails, user activity, and entity history'),
    ('AUDIT_LOG_ANALYTICS', 'View Audit Analytics', 'View system audit statistics, summaries, and activity metrics'),
    ('AUDIT_LOG_EXPORT', 'Export Audit Logs', 'Export system audit records to CSV and XLSX spreadsheets'),
    ('SECURITY_ACTIVITY_VIEW', 'View Security Activity', 'View dedicated security, authentication, and access control audit records')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    description = EXCLUDED.description;

-- SUPER_ADMIN & ADMIN: Grant all audit management permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'AUDIT_LOG_VIEW', 'AUDIT_LOG_ANALYTICS', 'AUDIT_LOG_EXPORT', 'SECURITY_ACTIVITY_VIEW'
)
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT DO NOTHING;
