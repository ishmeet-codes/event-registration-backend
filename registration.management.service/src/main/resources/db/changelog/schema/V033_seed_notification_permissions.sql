--liquibase formatted sql

--changeset agent:V033_seed_notification_permissions

-- Insert Notification Management permissions
INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('NOTIFICATION_VIEW', 'View Notifications', 'View in-app notifications and personal inbox'),
    ('NOTIFICATION_CREATE', 'Create Notification', 'Create and send a notification to a specific recipient'),
    ('NOTIFICATION_UPDATE', 'Update Notification', 'Update a notification record'),
    ('NOTIFICATION_DELETE', 'Delete Notification', 'Soft delete or dismiss notifications'),
    ('NOTIFICATION_BULK_SEND', 'Bulk Send Notifications', 'Send broadcast notifications to targeted groups or multiple recipients'),
    ('NOTIFICATION_SCHEDULE', 'Schedule Notifications', 'Schedule notifications for future delivery and manage scheduled jobs'),
    ('NOTIFICATION_VIEW_DELIVERY', 'View Delivery Details', 'Inspect delivery status, channel metrics, and dispatch outcomes'),
    ('NOTIFICATION_RETRY', 'Retry Notification', 'Manually retry failed notification deliveries'),
    ('NOTIFICATION_VIEW_ANALYTICS', 'View Notification Analytics', 'Access notification delivery summaries and statistics'),
    ('NOTIFICATION_VIEW_LOGS', 'View Notification Logs', 'Inspect delivery logs and audit records for communication channels'),
    ('NOTIFICATION_TEMPLATE_VIEW', 'View Templates', 'List and view notification templates'),
    ('NOTIFICATION_TEMPLATE_CREATE', 'Create Template', 'Create a new notification template with dynamic placeholders'),
    ('NOTIFICATION_TEMPLATE_UPDATE', 'Update Template', 'Edit existing notification templates'),
    ('NOTIFICATION_TEMPLATE_DELETE', 'Delete Template', 'Soft delete notification templates'),
    ('NOTIFICATION_PREFERENCE_VIEW', 'View Notification Preferences', 'View personal notification channel and topic preferences'),
    ('NOTIFICATION_PREFERENCE_UPDATE', 'Update Notification Preferences', 'Update personal notification channel and topic preferences')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    description = EXCLUDED.description;

-- SUPER_ADMIN & ADMIN: Grant all notification permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'NOTIFICATION_VIEW', 'NOTIFICATION_CREATE', 'NOTIFICATION_UPDATE', 'NOTIFICATION_DELETE',
    'NOTIFICATION_BULK_SEND', 'NOTIFICATION_SCHEDULE', 'NOTIFICATION_VIEW_DELIVERY', 'NOTIFICATION_RETRY',
    'NOTIFICATION_VIEW_ANALYTICS', 'NOTIFICATION_VIEW_LOGS',
    'NOTIFICATION_TEMPLATE_VIEW', 'NOTIFICATION_TEMPLATE_CREATE', 'NOTIFICATION_TEMPLATE_UPDATE', 'NOTIFICATION_TEMPLATE_DELETE',
    'NOTIFICATION_PREFERENCE_VIEW', 'NOTIFICATION_PREFERENCE_UPDATE'
)
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT DO NOTHING;

-- EVENT_MANAGER: Operational and broadcast notification permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'NOTIFICATION_VIEW', 'NOTIFICATION_CREATE', 'NOTIFICATION_UPDATE', 'NOTIFICATION_DELETE',
    'NOTIFICATION_BULK_SEND', 'NOTIFICATION_SCHEDULE', 'NOTIFICATION_VIEW_DELIVERY', 'NOTIFICATION_RETRY',
    'NOTIFICATION_VIEW_ANALYTICS', 'NOTIFICATION_TEMPLATE_VIEW',
    'NOTIFICATION_PREFERENCE_VIEW', 'NOTIFICATION_PREFERENCE_UPDATE'
)
WHERE r.role_code = 'EVENT_MANAGER'
ON CONFLICT DO NOTHING;

-- SCHOOL_STAFF, LOGIN_TEACHER, ACCOMPANYING_TEACHER: Standard notification access and preferences
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'NOTIFICATION_VIEW', 'NOTIFICATION_DELETE', 'NOTIFICATION_PREFERENCE_VIEW', 'NOTIFICATION_PREFERENCE_UPDATE'
)
WHERE r.role_code IN ('SCHOOL_STAFF', 'LOGIN_TEACHER', 'ACCOMPANYING_TEACHER', 'CHECKIN_TEAM')
ON CONFLICT DO NOTHING;
