package com.registration.management.registration.exception;

public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException(String message) {
        super(message);
    }

    public RegistrationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
