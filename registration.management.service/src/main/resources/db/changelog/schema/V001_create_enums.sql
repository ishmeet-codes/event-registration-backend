--liquibase formatted sql

--changeset ishmeet:V001_create_enums

------------------------------------------------------------
-- Staff Role
------------------------------------------------------------
CREATE TYPE staff_role AS ENUM (
    'LOGIN_TEACHER',
    'ACCOMPANYING_TEACHER'
);

------------------------------------------------------------
-- Registration Status
------------------------------------------------------------
CREATE TYPE registration_status AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'APPROVED',
    'REJECTED'
);

------------------------------------------------------------
-- Gender
------------------------------------------------------------
CREATE TYPE gender AS ENUM (
    'MALE',
    'FEMALE',
    'OTHER'
);

------------------------------------------------------------
-- Audit Action
------------------------------------------------------------
CREATE TYPE audit_action AS ENUM (
    'CREATE',
    'UPDATE',
    'DELETE',
    'LOGIN',
    'LOGOUT',
    'CHECKIN',
    'EXPORT',
    'PASSWORD_RESET',
    'ROLE_CHANGED'
);

------------------------------------------------------------
-- Audit Status
------------------------------------------------------------
CREATE TYPE audit_status AS ENUM (
    'SUCCESS',
    'FAILED'
);