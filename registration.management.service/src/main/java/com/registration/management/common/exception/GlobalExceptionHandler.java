package com.registration.management.common.exception;

import com.registration.management.registration.exception.RegistrationConflictException;
import com.registration.management.registration.exception.RegistrationHasParticipantsException;
import com.registration.management.registration.exception.RegistrationNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password"));
    }

    @ExceptionHandler(StaffConflictException.class)
    public ResponseEntity<Map<String, String>> handleStaffConflictException(StaffConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RegistrationHasParticipantsException.class)
    public ResponseEntity<Map<String, String>> handleRegistrationHasParticipantsException(RegistrationHasParticipantsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<Map<String, String>> handleRegistrationConflictException(RegistrationConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RegistrationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRegistrationNotFoundException(RegistrationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(com.registration.management.participant.exception.ParticipantNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleParticipantNotFoundException(com.registration.management.participant.exception.ParticipantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(com.registration.management.participant.exception.ParticipantLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleParticipantLimitExceededException(com.registration.management.participant.exception.ParticipantLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(com.registration.management.participant.exception.ParticipantHasCheckinRecordsException.class)
    public ResponseEntity<Map<String, String>> handleParticipantHasCheckinRecordsException(com.registration.management.participant.exception.ParticipantHasCheckinRecordsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SchoolCodeException.class)
    public ResponseEntity<Map<String, String>> handleSchoolCodeException(SchoolCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SchoolNotActiveException.class)
    public ResponseEntity<Map<String, String>> handleSchoolNotActiveException(SchoolNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(StaffRoleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleStaffRoleNotFoundException(StaffRoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid or malformed JSON payload"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("errors", errors));
    }
}
