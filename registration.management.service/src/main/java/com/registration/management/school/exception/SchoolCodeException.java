package com.registration.management.school.exception;

public class SchoolCodeException extends RuntimeException {

    public SchoolCodeException(String message) {
        super(message);
    }

    public SchoolCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
