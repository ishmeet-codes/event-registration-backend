package com.registration.management.checkin.exception;

public class InvalidCheckinStatusException extends RuntimeException {

    public InvalidCheckinStatusException(String message) {
        super(message);
    }

    public InvalidCheckinStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
