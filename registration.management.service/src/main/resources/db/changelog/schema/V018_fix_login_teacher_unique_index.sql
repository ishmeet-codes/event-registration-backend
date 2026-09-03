--liquibase formatted sql

--changeset ishmeet:V018_fix_login_teacher_unique_index

-- Drop the overly restrictive index that prevented creating a new login teacher even if the old one was inactive
DROP INDEX uq_school_login_teacher;

-- Recreate it with the active = true condition
CREATE UNIQUE INDEX uq_school_login_teacher
    ON school_staff(school_id)
    WHERE staff_role = 'LOGIN_TEACHER' AND active = true;
