package com.registration.management.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter
public class EventRequest {
    @NotBlank @Size(max = 50) private String participationCategoryCode;
    @NotBlank @Size(max = 150) private String eventName;
    @NotBlank private String description;
    @NotBlank @Size(max = 120) private String venue;
    @NotNull private LocalDateTime registrationDeadline;
    @NotNull private LocalDate eventDate;
    @NotNull private LocalTime startTime;
    @NotNull private LocalTime endTime;
    @NotNull @Positive private Integer maxRegistrations;
}
