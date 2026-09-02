package com.registration.management.school.exception;

public class StaffRoleNotFoundException extends RuntimeException {

    public StaffRoleNotFoundException(String message) {
        super(message);
    }

    public StaffRoleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
