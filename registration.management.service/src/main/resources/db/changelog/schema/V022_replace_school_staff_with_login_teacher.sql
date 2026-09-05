--liquibase formatted sql

--changeset assistant:V022_replace_school_staff_with_login_teacher

-- 1. Migrate any existing users with role 'SCHOOL_STAFF' to 'LOGIN_TEACHER'
UPDATE users 
SET role_id = (SELECT id FROM roles WHERE role_code = 'LOGIN_TEACHER')
WHERE role_id = (SELECT id FROM roles WHERE role_code = 'SCHOOL_STAFF');

-- 2. Remove role permissions associated with 'SCHOOL_STAFF'
DELETE FROM role_permissions 
WHERE role_id = (SELECT id FROM roles WHERE role_code = 'SCHOOL_STAFF');

-- 3. Delete the 'SCHOOL_STAFF' role from the roles table
DELETE FROM roles 
WHERE role_code = 'SCHOOL_STAFF';
