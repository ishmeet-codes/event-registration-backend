package com.registration.management.school.exception;

import com.registration.management.common.exception.GlobalExceptionHandler;
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
