package com.registration.management.refreshment.dto;

import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.refreshment.enums.TrackingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshmentSessionRequestDTO {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Session name is required")
    @Size(max = 255)
    private String name;

    private Long eventId;

    @NotNull(message = "Recipient category is required")
    private RecipientCategory recipientCategory;

    @Builder.Default
    private TrackingType trackingType = TrackingType.INDIVIDUAL;

    @Builder.Default
    private Integer expectedCount = 0;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Builder.Default
    private Integer displayOrder = 1;

    @Builder.Default
    private Boolean requireCheckinPresence = true;

    @Builder.Default
    private Boolean active = true;
}
