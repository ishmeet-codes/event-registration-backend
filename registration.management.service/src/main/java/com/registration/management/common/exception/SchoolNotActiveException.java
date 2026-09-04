package com.registration.management.common.exception;

public class SchoolNotActiveException extends RuntimeException {

    public SchoolNotActiveException(String message) {
        super(message);
    }

    public SchoolNotActiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
