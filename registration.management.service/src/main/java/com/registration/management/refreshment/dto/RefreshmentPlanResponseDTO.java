package com.registration.management.refreshment.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshmentPlanResponseDTO {
    private Long id;
    private String name;
    private LocalDate eventDate;
    private String description;
    private Boolean active;
    private Integer totalSessions;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
