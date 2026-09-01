package com.registration.management.event.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Builder
public class EventResponse {
    private final Long id;
    private final ParticipationCategoryResponse participationCategory;
    private final String eventName;
    private final String description;
    private final String venue;
    private final LocalDateTime registrationDeadline;
    private final LocalDate eventDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer maxRegistrations;
    private final long currentRegistrations;
    private final long remainingSlots;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
