--liquibase formatted sql

--changeset agent:V037_seed_campaign_permissions

-- Insert Email Campaign Management permissions
INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('EMAIL_CAMPAIGN_VIEW', 'View Email Campaigns', 'View and inspect bulk email campaigns, audiences, and delivery logs'),
    ('EMAIL_CAMPAIGN_CREATE', 'Create Email Campaigns', 'Draft and configure email campaigns, custom lists, and audience filters'),
    ('EMAIL_CAMPAIGN_SEND', 'Send Email Campaigns', 'Execute immediate email campaign dispatches'),
    ('EMAIL_CAMPAIGN_SCHEDULE', 'Schedule Email Campaigns', 'Schedule email campaign delivery for future background dispatch'),
    ('EMAIL_CAMPAIGN_CANCEL', 'Cancel Email Campaigns', 'Cancel scheduled email campaigns'),
    ('EMAIL_AUTOMATION_VIEW', 'View Email Automations', 'View automated email trigger workflows'),
    ('EMAIL_AUTOMATION_CREATE', 'Create Email Automations', 'Configure event-triggered automated email workflows')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    description = EXCLUDED.description;

-- SUPER_ADMIN & ADMIN: Grant all campaign management permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'EMAIL_CAMPAIGN_VIEW', 'EMAIL_CAMPAIGN_CREATE', 'EMAIL_CAMPAIGN_SEND',
    'EMAIL_CAMPAIGN_SCHEDULE', 'EMAIL_CAMPAIGN_CANCEL', 'EMAIL_AUTOMATION_VIEW', 'EMAIL_AUTOMATION_CREATE'
)
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT DO NOTHING;
