package com.registration.management.participant.exception;

public class ParticipantLimitExceededException extends RuntimeException {

    public ParticipantLimitExceededException(String message) {
        super(message);
    }

    public ParticipantLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
