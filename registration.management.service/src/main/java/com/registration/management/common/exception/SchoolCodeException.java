package com.registration.management.common.exception;

public class SchoolCodeException extends RuntimeException {

    public SchoolCodeException(String message) {
        super(message);
    }

    public SchoolCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
