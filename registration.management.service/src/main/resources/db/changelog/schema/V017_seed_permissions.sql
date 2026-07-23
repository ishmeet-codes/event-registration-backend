--liquibase formatted sql

--changeset ishmeet:V017_seed_permissions

INSERT INTO permissions
(permission_code, permission_name, description)
VALUES

-- Roles
('ROLE_READ', 'View Roles', 'View roles'),
('ROLE_CREATE', 'Create Roles', 'Create new roles'),
('ROLE_UPDATE', 'Update Roles', 'Update existing roles'),
('ROLE_DELETE', 'Delete Roles', 'Delete roles'),

-- Users
('USER_READ', 'View Users', 'View users'),
('USER_CREATE', 'Create Users', 'Create users'),
('USER_UPDATE', 'Update Users', 'Update users'),
('USER_DELETE', 'Delete Users', 'Delete users'),

-- Schools
('SCHOOL_READ', 'View Schools', 'View schools'),
('SCHOOL_CREATE', 'Create Schools', 'Create schools'),
('SCHOOL_UPDATE', 'Update Schools', 'Update schools'),
('SCHOOL_DELETE', 'Delete Schools', 'Delete schools'),

-- Events
('EVENT_READ', 'View Events', 'View events'),
('EVENT_CREATE', 'Create Events', 'Create events'),
('EVENT_UPDATE', 'Update Events', 'Update events'),
('EVENT_DELETE', 'Delete Events', 'Delete events'),

-- Registrations
('REGISTRATION_READ', 'View Registrations', 'View registrations'),
('REGISTRATION_CREATE', 'Create Registrations', 'Create registrations'),
('REGISTRATION_UPDATE', 'Update Registrations', 'Update registrations'),
('REGISTRATION_DELETE', 'Delete Registrations', 'Delete registrations'),
('REGISTRATION_APPROVE', 'Approve Registrations', 'Approve registrations'),
('REGISTRATION_REJECT', 'Reject Registrations', 'Reject registrations'),

-- Participants
('PARTICIPANT_READ', 'View Participants', 'View participants'),
('PARTICIPANT_CREATE', 'Create Participants', 'Create participants'),
('PARTICIPANT_UPDATE', 'Update Participants', 'Update participants'),
('PARTICIPANT_DELETE', 'Delete Participants', 'Delete participants'),

-- Check-ins
('CHECKIN_READ', 'View Check-ins', 'View check-ins'),
('CHECKIN_CREATE', 'Create Check-ins', 'Perform check-ins'),

-- Certificates
('CERTIFICATE_GENERATE', 'Generate Certificates', 'Generate certificates'),
('CERTIFICATE_DOWNLOAD', 'Download Certificates', 'Download certificates'),

-- Dashboard
('DASHBOARD_VIEW', 'View Dashboard', 'View dashboard'),

-- Audit
('AUDIT_VIEW', 'View Audit Logs', 'View audit logs'),

-- Reports
('REPORT_EXPORT', 'Export Reports', 'Export reports'),

-- Profile
('PROFILE_UPDATE', 'Update Profile', 'Update own profile'),

-- System
('SYSTEM_SETTINGS', 'System Settings', 'Manage system settings')

    ON CONFLICT (permission_code) DO NOTHING;