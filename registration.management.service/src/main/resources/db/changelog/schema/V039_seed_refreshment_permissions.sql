--liquibase formatted sql

--changeset agent:V039_seed_refreshment_permissions

-- ==============================================================================
-- Migration V039: Seed Refreshment Management Permissions and Roles
-- ==============================================================================

-- 1. Ensure REFRESHMENT_TEAM role exists
INSERT INTO roles (role_code, role_name, description, system_role, active)
VALUES
    ('REFRESHMENT_TEAM', 'Refreshment Team', 'Field operations team responsible for physical distribution of refreshments to participants, staff, OC members, and guests.', TRUE, TRUE)
ON CONFLICT (role_code) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    active = EXCLUDED.active;

-- 2. Insert Refreshment permissions
INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('REFRESHMENT_VIEW', 'View Refreshment', 'View refreshment plans, sessions, dashboard summaries and progress'),
    ('REFRESHMENT_CREATE', 'Create Refreshment Plan', 'Create refreshment plans and session schedules'),
    ('REFRESHMENT_UPDATE', 'Update Refreshment Plan', 'Update refreshment plans and session configurations'),
    ('REFRESHMENT_DELETE', 'Delete Refreshment Plan', 'Delete refreshment plans and sessions'),
    ('REFRESHMENT_SESSION_MANAGE', 'Manage Refreshment Sessions', 'Create, update, and manage distribution sessions'),
    ('REFRESHMENT_TEAM_MANAGE', 'Manage Refreshment Teams', 'Create and manage refreshment teams and volunteer memberships'),
    ('REFRESHMENT_ASSIGNMENT_MANAGE', 'Manage Refreshment Assignments', 'Assign teams to events and sessions'),
    ('REFRESHMENT_DISTRIBUTE', 'Distribute Refreshment', 'Mark refreshments as given to individuals or log guest count'),
    ('REFRESHMENT_DISTRIBUTION_VIEW', 'View Refreshment Distributions', 'View distribution logs and recipient statuses'),
    ('REFRESHMENT_DISTRIBUTION_CORRECT', 'Correct Refreshment Distribution', 'Correct / revoke accidental distribution marks with audit remarks'),
    ('REFRESHMENT_REPORT', 'Export Refreshment Reports', 'Export refreshment operations reports and logs')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    description = EXCLUDED.description;

-- 3. Grant FULL Refreshment permissions to SUPER_ADMIN, ADMIN, and EVENT_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'REFRESHMENT_VIEW',
    'REFRESHMENT_CREATE',
    'REFRESHMENT_UPDATE',
    'REFRESHMENT_DELETE',
    'REFRESHMENT_SESSION_MANAGE',
    'REFRESHMENT_TEAM_MANAGE',
    'REFRESHMENT_ASSIGNMENT_MANAGE',
    'REFRESHMENT_DISTRIBUTE',
    'REFRESHMENT_DISTRIBUTION_VIEW',
    'REFRESHMENT_DISTRIBUTION_CORRECT',
    'REFRESHMENT_REPORT'
)
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN', 'EVENT_MANAGER')
ON CONFLICT DO NOTHING;

-- 4. Grant Field Distribution permissions to REFRESHMENT_TEAM role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.permission_code IN (
    'REFRESHMENT_VIEW',
    'REFRESHMENT_DISTRIBUTE',
    'REFRESHMENT_DISTRIBUTION_VIEW'
)
WHERE r.role_code = 'REFRESHMENT_TEAM'
ON CONFLICT DO NOTHING;
