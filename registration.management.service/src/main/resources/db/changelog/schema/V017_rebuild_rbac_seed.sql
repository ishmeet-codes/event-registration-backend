--liquibase formatted sql

--changeset codex:V026_rebuild_rbac_seed

INSERT INTO roles (role_code, role_name, description, system_role, active)
VALUES
    ('SUPER_ADMIN', 'Super Administrator', 'Complete access to the entire Registration Management System.', TRUE, TRUE),
    ('ADMIN', 'Administrator', 'Administrative management of users, schools, events, registrations, participants, and related configuration.', TRUE, TRUE),
    ('EVENT_MANAGER', 'Event Manager', 'Manages events, participation categories, registrations, and participants.', TRUE, TRUE),
    ('SCHOOL_STAFF', 'School Staff', 'School-level operational user responsible for school registrations, participants, and related school operations.', TRUE, TRUE),
    ('VIEWER', 'Viewer', 'Read-only access to the current system modules.', TRUE, TRUE),
    ('PARTICIPANT', 'Participant', 'Participant-facing role with limited access to event, registration, and participant information.', TRUE, TRUE)
ON CONFLICT (role_code) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    system_role = EXCLUDED.system_role,
    active = EXCLUDED.active;

-- Move existing accounts off roles that are being removed before deleting those roles.
UPDATE users
SET role_id = (SELECT id FROM roles WHERE role_code = 'EVENT_MANAGER')
WHERE role_id IN (SELECT id FROM roles WHERE role_code = 'EVENT_ORGANIZER');

UPDATE users
SET role_id = (SELECT id FROM roles WHERE role_code = 'SCHOOL_STAFF')
WHERE role_id IN (SELECT id FROM roles WHERE role_code = 'SCHOOL_INCHARGE');

UPDATE users
SET role_id = (SELECT id FROM roles WHERE role_code = 'PARTICIPANT')
WHERE role_id IN (SELECT id FROM roles WHERE role_code IN ('ATTENDEE', 'GUEST'));

UPDATE users
SET role_id = (SELECT id FROM roles WHERE role_code = 'VIEWER')
WHERE role_id IN (SELECT id FROM roles WHERE role_code IN ('CHECKIN_TEAM', 'CERTIFICATE_TEAM'));

UPDATE users
SET role_id = (SELECT id FROM roles WHERE role_code = 'ADMIN')
WHERE role_id IN (SELECT id FROM roles WHERE role_code = 'REGISTRATION_TEAM');

DELETE FROM role_permissions;

DELETE FROM permissions
WHERE permission_code NOT IN (
    'USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE', 'USER_STATUS_UPDATE',
    'ROLE_VIEW', 'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_ASSIGN',
    'PERMISSION_VIEW', 'PERMISSION_CREATE', 'PERMISSION_UPDATE', 'PERMISSION_DELETE',
    'PROFILE_UPDATE',
    'SCHOOL_VIEW', 'SCHOOL_CREATE', 'SCHOOL_UPDATE', 'SCHOOL_DELETE', 'SCHOOL_STATUS_UPDATE',
    'SCHOOL_STAFF_VIEW', 'SCHOOL_STAFF_CREATE', 'SCHOOL_STAFF_UPDATE', 'SCHOOL_STAFF_DELETE', 'SCHOOL_STAFF_STATUS_UPDATE',
    'EVENT_VIEW', 'EVENT_CREATE', 'EVENT_UPDATE', 'EVENT_DELETE', 'EVENT_STATUS_UPDATE',
    'PARTICIPATION_CATEGORY_VIEW', 'PARTICIPATION_CATEGORY_CREATE', 'PARTICIPATION_CATEGORY_UPDATE', 'PARTICIPATION_CATEGORY_DELETE', 'PARTICIPATION_CATEGORY_STATUS_UPDATE',
    'REGISTRATION_VIEW', 'REGISTRATION_CREATE', 'REGISTRATION_UPDATE', 'REGISTRATION_DELETE', 'REGISTRATION_STATUS_UPDATE',
    'PARTICIPANT_VIEW', 'PARTICIPANT_CREATE', 'PARTICIPANT_UPDATE', 'PARTICIPANT_DELETE'
);

DELETE FROM roles
WHERE role_code NOT IN ('SUPER_ADMIN', 'ADMIN', 'EVENT_MANAGER', 'SCHOOL_STAFF', 'VIEWER', 'PARTICIPANT');

INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('USER_VIEW', 'View Users', 'View users'), ('USER_CREATE', 'Create Users', 'Create users'), ('USER_UPDATE', 'Update Users', 'Update users'), ('USER_DELETE', 'Delete Users', 'Delete users'), ('USER_STATUS_UPDATE', 'Update User Status', 'Activate or deactivate users'),
    ('ROLE_VIEW', 'View Roles', 'View roles'), ('ROLE_CREATE', 'Create Roles', 'Create roles'), ('ROLE_UPDATE', 'Update Roles', 'Update roles'), ('ROLE_DELETE', 'Delete Roles', 'Delete roles'), ('ROLE_ASSIGN', 'Assign Roles', 'Assign roles to users'),
    ('PERMISSION_VIEW', 'View Permissions', 'View permissions'), ('PERMISSION_CREATE', 'Create Permissions', 'Create permissions'), ('PERMISSION_UPDATE', 'Update Permissions', 'Update permissions'), ('PERMISSION_DELETE', 'Delete Permissions', 'Delete permissions'), ('PROFILE_UPDATE', 'Update Profile', 'Update own profile'),
    ('SCHOOL_VIEW', 'View Schools', 'View schools'), ('SCHOOL_CREATE', 'Create Schools', 'Create schools'), ('SCHOOL_UPDATE', 'Update Schools', 'Update schools'), ('SCHOOL_DELETE', 'Delete Schools', 'Delete schools'), ('SCHOOL_STATUS_UPDATE', 'Update School Status', 'Activate or deactivate schools'),
    ('SCHOOL_STAFF_VIEW', 'View School Staff', 'View school staff'), ('SCHOOL_STAFF_CREATE', 'Create School Staff', 'Create school staff'), ('SCHOOL_STAFF_UPDATE', 'Update School Staff', 'Update school staff'), ('SCHOOL_STAFF_DELETE', 'Delete School Staff', 'Delete school staff'), ('SCHOOL_STAFF_STATUS_UPDATE', 'Update School Staff Status', 'Activate or deactivate school staff'),
    ('EVENT_VIEW', 'View Events', 'View events'), ('EVENT_CREATE', 'Create Events', 'Create events'), ('EVENT_UPDATE', 'Update Events', 'Update events'), ('EVENT_DELETE', 'Delete Events', 'Delete events'), ('EVENT_STATUS_UPDATE', 'Update Event Status', 'Activate or deactivate events'),
    ('PARTICIPATION_CATEGORY_VIEW', 'View Participation Categories', 'View participation categories'), ('PARTICIPATION_CATEGORY_CREATE', 'Create Participation Categories', 'Create participation categories'), ('PARTICIPATION_CATEGORY_UPDATE', 'Update Participation Categories', 'Update participation categories'), ('PARTICIPATION_CATEGORY_DELETE', 'Delete Participation Categories', 'Delete participation categories'), ('PARTICIPATION_CATEGORY_STATUS_UPDATE', 'Update Participation Category Status', 'Activate or deactivate participation categories'),
    ('REGISTRATION_VIEW', 'View Registrations', 'View registrations'), ('REGISTRATION_CREATE', 'Create Registrations', 'Create registrations'), ('REGISTRATION_UPDATE', 'Update Registrations', 'Update registrations'), ('REGISTRATION_DELETE', 'Delete Registrations', 'Delete registrations'), ('REGISTRATION_STATUS_UPDATE', 'Update Registration Status', 'Update registration status'),
    ('PARTICIPANT_VIEW', 'View Participants', 'View participants'), ('PARTICIPANT_CREATE', 'Create Participants', 'Create participants'), ('PARTICIPANT_UPDATE', 'Update Participants', 'Update participants'), ('PARTICIPANT_DELETE', 'Delete Participants', 'Delete participants')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    description = EXCLUDED.description;

-- Recreate mappings from the final role model. SUPER_ADMIN and ADMIN receive the complete set.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'EVENT_VIEW', 'EVENT_CREATE', 'EVENT_UPDATE', 'EVENT_DELETE', 'EVENT_STATUS_UPDATE',
    'PARTICIPATION_CATEGORY_VIEW', 'PARTICIPATION_CATEGORY_CREATE', 'PARTICIPATION_CATEGORY_UPDATE', 'PARTICIPATION_CATEGORY_DELETE', 'PARTICIPATION_CATEGORY_STATUS_UPDATE',
    'REGISTRATION_VIEW', 'REGISTRATION_CREATE', 'REGISTRATION_UPDATE', 'REGISTRATION_DELETE', 'REGISTRATION_STATUS_UPDATE',
    'PARTICIPANT_VIEW', 'PARTICIPANT_CREATE', 'PARTICIPANT_UPDATE', 'PARTICIPANT_DELETE', 'PROFILE_UPDATE'
)
WHERE r.role_code = 'EVENT_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'SCHOOL_VIEW', 'SCHOOL_CREATE', 'SCHOOL_UPDATE', 'SCHOOL_DELETE', 'SCHOOL_STATUS_UPDATE',
    'SCHOOL_STAFF_VIEW', 'SCHOOL_STAFF_CREATE', 'SCHOOL_STAFF_UPDATE', 'SCHOOL_STAFF_DELETE', 'SCHOOL_STAFF_STATUS_UPDATE',
    'EVENT_VIEW', 'REGISTRATION_VIEW', 'REGISTRATION_CREATE', 'REGISTRATION_UPDATE', 'REGISTRATION_DELETE', 'REGISTRATION_STATUS_UPDATE',
    'PARTICIPANT_VIEW', 'PARTICIPANT_CREATE', 'PARTICIPANT_UPDATE', 'PROFILE_UPDATE'
)
WHERE r.role_code = 'SCHOOL_STAFF';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'USER_VIEW', 'ROLE_VIEW', 'PERMISSION_VIEW', 'SCHOOL_VIEW', 'SCHOOL_STAFF_VIEW',
    'EVENT_VIEW', 'PARTICIPATION_CATEGORY_VIEW', 'REGISTRATION_VIEW', 'PARTICIPANT_VIEW', 'PROFILE_UPDATE'
)
WHERE r.role_code = 'VIEWER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN ('EVENT_VIEW', 'REGISTRATION_VIEW', 'PARTICIPANT_VIEW', 'PROFILE_UPDATE')
WHERE r.role_code = 'PARTICIPANT';
