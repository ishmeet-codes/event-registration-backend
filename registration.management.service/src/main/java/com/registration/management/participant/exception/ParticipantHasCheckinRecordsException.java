package com.registration.management.participant.exception;

public class ParticipantHasCheckinRecordsException extends RuntimeException {

    public ParticipantHasCheckinRecordsException(String message) {
        super(message);
    }

    public ParticipantHasCheckinRecordsException(String message, Throwable cause) {
        super(message, cause);
    }
}
