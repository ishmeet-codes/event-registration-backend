package com.registration.management.common.exception;

public class StaffRoleNotFoundException extends RuntimeException {

    public StaffRoleNotFoundException(String message) {
        super(message);
    }

    public StaffRoleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
