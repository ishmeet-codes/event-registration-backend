--liquibase formatted sql

--changeset ishmeet:V018_seed_role_permissions

--------------------------------------------------------
-- SUPER_ADMIN
--------------------------------------------------------

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.role_code='SUPER_ADMIN'
    ON CONFLICT DO NOTHING;

--------------------------------------------------------
-- ADMIN
--------------------------------------------------------
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id,p.id
FROM roles r
         JOIN permissions p
              ON p.permission_code IN (

                                       'ROLE_READ',

                                       'USER_READ',
                                       'USER_CREATE',
                                       'USER_UPDATE',

                                       'SCHOOL_READ',
                                       'SCHOOL_CREATE',
                                       'SCHOOL_UPDATE',
                                       'SCHOOL_DELETE',

                                       'EVENT_READ',
                                       'EVENT_CREATE',
                                       'EVENT_UPDATE',
                                       'EVENT_DELETE',

                                       'REGISTRATION_READ',
                                       'REGISTRATION_CREATE',
                                       'REGISTRATION_UPDATE',
                                       'REGISTRATION_APPROVE',
                                       'REGISTRATION_REJECT',

                                       'PARTICIPANT_READ',
                                       'PARTICIPANT_CREATE',
                                       'PARTICIPANT_UPDATE',
                                       'PARTICIPANT_DELETE',

                                       'CHECKIN_READ',
                                       'CHECKIN_CREATE',

                                       'CERTIFICATE_GENERATE',
                                       'CERTIFICATE_DOWNLOAD',

                                       'DASHBOARD_VIEW',

                                       'AUDIT_VIEW',

                                       'REPORT_EXPORT',

                                       'PROFILE_UPDATE'

                  )
WHERE r.role_code='ADMIN'
    ON CONFLICT DO NOTHING;

--------------------------------------------------------
-- REGISTRATION TEAM
--------------------------------------------------------
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id,p.id
FROM roles r
         JOIN permissions p
              ON p.permission_code IN (

                                       'SCHOOL_READ',

                                       'EVENT_READ',

                                       'REGISTRATION_READ',
                                       'REGISTRATION_CREATE',
                                       'REGISTRATION_UPDATE',

                                       'PARTICIPANT_READ',
                                       'PARTICIPANT_CREATE',
                                       'PARTICIPANT_UPDATE',

                                       'DASHBOARD_VIEW',

                                       'PROFILE_UPDATE'

                  )
WHERE r.role_code='REGISTRATION_TEAM'
    ON CONFLICT DO NOTHING;

--------------------------------------------------------
-- CHECKIN TEAM
--------------------------------------------------------

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id,p.id
FROM roles r
         JOIN permissions p
              ON p.permission_code IN (

                                       'EVENT_READ',

                                       'PARTICIPANT_READ',

                                       'CHECKIN_READ',
                                       'CHECKIN_CREATE',

                                       'DASHBOARD_VIEW',

                                       'PROFILE_UPDATE'

                  )
WHERE r.role_code='CHECKIN_TEAM'
    ON CONFLICT DO NOTHING;

--------------------------------------------------------
-- CERTIFICATE TEAM
--------------------------------------------------------
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id,p.id
FROM roles r
         JOIN permissions p
              ON p.permission_code IN (

                                       'EVENT_READ',

                                       'PARTICIPANT_READ',

                                       'CERTIFICATE_GENERATE',
                                       'CERTIFICATE_DOWNLOAD',

                                       'REPORT_EXPORT',

                                       'DASHBOARD_VIEW',

                                       'PROFILE_UPDATE'

                  )
WHERE r.role_code='CERTIFICATE_TEAM'
    ON CONFLICT DO NOTHING;