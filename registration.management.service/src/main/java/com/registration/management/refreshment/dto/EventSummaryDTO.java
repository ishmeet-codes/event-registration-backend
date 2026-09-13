package com.registration.management.refreshment.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryDTO {
    private Long eventId;
    private String eventName;
    private Long eligible;
    private Long present;
    private Long distributed;
    private Long pending;
    private Double distributionRate;
}
