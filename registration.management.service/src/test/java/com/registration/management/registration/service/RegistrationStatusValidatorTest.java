package com.registration.management.registration.service;

import com.registration.management.enums.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationStatusValidatorTest {

    private RegistrationStatusValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RegistrationStatusValidator();
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, PENDING",
            "DRAFT, CANCELLED",
            "PENDING, APPROVED",
            "PENDING, REJECTED",
            "PENDING, CANCELLED",
            "SUBMITTED, APPROVED",
            "SUBMITTED, REJECTED",
            "SUBMITTED, CANCELLED",
            "APPROVED, CANCELLED",
            "APPROVED, COMPLETED",
            "DRAFT, DRAFT",
            "PENDING, PENDING",
            "APPROVED, APPROVED"
    })
    void validateTransition_validTransitions_shouldSucceed(RegistrationStatus current, RegistrationStatus target) {
        assertDoesNotThrow(() -> validator.validateTransition(current, target));
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, APPROVED",
            "DRAFT, REJECTED",
            "DRAFT, COMPLETED",
            "PENDING, COMPLETED",
            "PENDING, DRAFT",
            "APPROVED, DRAFT",
            "APPROVED, PENDING",
            "APPROVED, REJECTED",
            "REJECTED, DRAFT",
            "REJECTED, PENDING",
            "REJECTED, APPROVED",
            "CANCELLED, DRAFT",
            "CANCELLED, PENDING",
            "CANCELLED, APPROVED",
            "COMPLETED, DRAFT",
            "COMPLETED, PENDING",
            "COMPLETED, APPROVED",
            "COMPLETED, CANCELLED"
    })
    void validateTransition_invalidTransitions_shouldThrowResponseStatusException(RegistrationStatus current, RegistrationStatus target) {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> validator.validateTransition(current, target)
        );
        assertTrue(ex.getMessage().contains("Invalid status transition"));
    }

    @Test
    void validateTransition_nullStatus_shouldThrowResponseStatusException() {
        assertThrows(ResponseStatusException.class, () -> validator.validateTransition(null, RegistrationStatus.PENDING));
        assertThrows(ResponseStatusException.class, () -> validator.validateTransition(RegistrationStatus.DRAFT, null));
    }
}
