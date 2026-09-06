package com.registration.management.checkin.exception;

public class CheckinNotFoundException extends RuntimeException {

    public CheckinNotFoundException(String message) {
        super(message);
    }

    public CheckinNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
