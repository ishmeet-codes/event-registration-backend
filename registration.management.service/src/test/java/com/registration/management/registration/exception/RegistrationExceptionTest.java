package com.registration.management.registration.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationExceptionTest {

    @Test
    void testRegistrationHasParticipantsException() {
        RegistrationHasParticipantsException ex = new RegistrationHasParticipantsException("Cannot delete registration with participants");
        assertEquals("Cannot delete registration with participants", ex.getMessage());

        Throwable cause = new RuntimeException("Underlying cause");
        RegistrationHasParticipantsException exWithCause = new RegistrationHasParticipantsException("Error occurred", cause);
        assertEquals("Error occurred", exWithCause.getMessage());
        assertEquals(cause, exWithCause.getCause());
    }

    @Test
    void testRegistrationConflictException() {
        RegistrationConflictException ex = new RegistrationConflictException("Registration already exists");
        assertEquals("Registration already exists", ex.getMessage());

        Throwable cause = new RuntimeException("Underlying cause");
        RegistrationConflictException exWithCause = new RegistrationConflictException("Conflict error", cause);
        assertEquals("Conflict error", exWithCause.getMessage());
        assertEquals(cause, exWithCause.getCause());
    }

    @Test
    void testRegistrationNotFoundException() {
        RegistrationNotFoundException ex = new RegistrationNotFoundException("Registration not found: 101");
        assertEquals("Registration not found: 101", ex.getMessage());

        Throwable cause = new RuntimeException("Underlying cause");
        RegistrationNotFoundException exWithCause = new RegistrationNotFoundException("Not found", cause);
        assertEquals("Not found", exWithCause.getMessage());
        assertEquals(cause, exWithCause.getCause());
    }
}
