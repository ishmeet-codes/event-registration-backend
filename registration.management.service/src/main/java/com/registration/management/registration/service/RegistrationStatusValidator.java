package com.registration.management.registration.service;

import com.registration.management.enums.RegistrationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class RegistrationStatusValidator {

    private static final Map<RegistrationStatus, Set<RegistrationStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(RegistrationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(RegistrationStatus.DRAFT, EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.SUBMITTED, RegistrationStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RegistrationStatus.PENDING, EnumSet.of(RegistrationStatus.APPROVED, RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RegistrationStatus.SUBMITTED, EnumSet.of(RegistrationStatus.APPROVED, RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RegistrationStatus.APPROVED, EnumSet.of(RegistrationStatus.CANCELLED, RegistrationStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(RegistrationStatus.REJECTED, Collections.emptySet());
        ALLOWED_TRANSITIONS.put(RegistrationStatus.CANCELLED, Collections.emptySet());
        ALLOWED_TRANSITIONS.put(RegistrationStatus.COMPLETED, Collections.emptySet());
    }

    public void validateTransition(RegistrationStatus currentStatus, RegistrationStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status cannot be null");
        }

        if (currentStatus == targetStatus) {
            return; // No transition needed
        }

        Set<RegistrationStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        if (!allowed.contains(targetStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Invalid status transition from %s to %s", currentStatus, targetStatus)
            );
        }
    }
}
