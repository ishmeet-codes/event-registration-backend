package com.registration.management.school.exception;

public class SchoolNotActiveException extends RuntimeException {

    public SchoolNotActiveException(String message) {
        super(message);
    }

    public SchoolNotActiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
