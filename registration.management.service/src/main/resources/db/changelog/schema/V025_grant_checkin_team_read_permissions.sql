--liquibase formatted sql

--changeset agent:V025_grant_checkin_team_read_permissions

-- Grant READ-ONLY access to Schools, Events, Registrations & Participants to Check-in Team
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'SCHOOL_VIEW', 'EVENT_VIEW', 'REGISTRATION_VIEW', 'PARTICIPANT_VIEW'
)
WHERE r.role_code = 'CHECKIN_TEAM'
ON CONFLICT DO NOTHING;
