--liquibase formatted sql

--changeset ishmeet:V016_seed_roles

INSERT INTO roles (role_code, role_name, description, system_role, active)
VALUES
    ('SUPER_ADMIN', 'Super Administrator', 'Full system access', TRUE, TRUE),
    ('ADMIN', 'Administrator', 'Application administrator', TRUE, TRUE),
    ('REGISTRATION_TEAM', 'Registration Team', 'Manage school and participant registrations', TRUE, TRUE),
    ('CHECKIN_TEAM', 'Check-in Team', 'Manage participant and teacher check-ins', TRUE, TRUE),
    ('CERTIFICATE_TEAM', 'Certificate Team', 'Generate and manage certificates', TRUE, TRUE)
    ON CONFLICT (role_code) DO NOTHING;