package com.registration.management.registration.exception;

public class RegistrationHasParticipantsException extends RuntimeException {

    public RegistrationHasParticipantsException(String message) {
        super(message);
    }

    public RegistrationHasParticipantsException(String message, Throwable cause) {
        super(message, cause);
    }
}
