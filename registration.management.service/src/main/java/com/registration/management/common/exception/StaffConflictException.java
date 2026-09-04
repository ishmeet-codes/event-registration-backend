package com.registration.management.common.exception;

public class StaffConflictException extends RuntimeException {

    public StaffConflictException(String message) {
        super(message);
    }

    public StaffConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
