package com.registration.management.checkin.exception;

public class ParticipantAlreadyCheckedInException extends RuntimeException {

    public ParticipantAlreadyCheckedInException(String message) {
        super(message);
    }

    public ParticipantAlreadyCheckedInException(String message, Throwable cause) {
        super(message, cause);
    }
}
