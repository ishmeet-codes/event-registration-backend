package com.registration.management.school.exception;

import com.registration.management.common.exception.*;
import com.registration.management.registration.exception.RegistrationConflictException;
import com.registration.management.registration.exception.RegistrationHasParticipantsException;
import com.registration.management.registration.exception.RegistrationNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleSchoolCodeException_ReturnsBadRequest() {
        SchoolCodeException ex = new SchoolCodeException("School code already exists: SCH001");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleSchoolCodeException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("School code already exists: SCH001", response.getBody().get("error"));
    }

    @Test
    void handleSchoolNotActiveException_ReturnsBadRequest() {
        SchoolNotActiveException ex = new SchoolNotActiveException("School is not active with id: 1");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleSchoolNotActiveException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("School is not active with id: 1", response.getBody().get("error"));
    }

    @Test
    void handleStaffRoleNotFoundException_ReturnsBadRequest() {
        StaffRoleNotFoundException ex = new StaffRoleNotFoundException("Staff role is required");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleStaffRoleNotFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Staff role is required", response.getBody().get("error"));
    }

    @Test
    void handleStaffConflictException_ReturnsConflict() {
        StaffConflictException ex = new StaffConflictException("Staff conflict");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleStaffConflictException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Staff conflict", response.getBody().get("error"));
    }

    @Test
    void handleRegistrationHasParticipantsException_ReturnsConflict() {
        RegistrationHasParticipantsException ex = new RegistrationHasParticipantsException("Cannot delete registration");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleRegistrationHasParticipantsException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Cannot delete registration", response.getBody().get("error"));
    }

    @Test
    void handleRegistrationConflictException_ReturnsConflict() {
        RegistrationConflictException ex = new RegistrationConflictException("Registration already exists");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleRegistrationConflictException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Registration already exists", response.getBody().get("error"));
    }

    @Test
    void handleRegistrationNotFoundException_ReturnsNotFound() {
        RegistrationNotFoundException ex = new RegistrationNotFoundException("Registration not found");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleRegistrationNotFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Registration not found", response.getBody().get("error"));
    }

    @Test
    void handleResourceNotFoundException_ReturnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleResourceNotFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody().get("error"));
    }

    @Test
    void handleBadCredentialsException_ReturnsUnauthorized() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleBadCredentialsException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid email or password", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgumentException_ReturnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid argument provided", response.getBody().get("error"));
    }

    @Test
    void handleEntityNotFoundException_ReturnsNotFound() {
        EntityNotFoundException ex = new EntityNotFoundException("Entity not found with ID 1");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleEntityNotFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Entity not found with ID 1", response.getBody().get("error"));
    }

    @Test
    void handleHttpMessageNotReadableException_ReturnsBadRequest() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", (HttpInputMessage) null);
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleHttpMessageNotReadableException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid or malformed JSON payload", response.getBody().get("error"));
    }
}
