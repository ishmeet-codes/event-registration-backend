package com.registration.management.event.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class ParticipationCategoryResponse {
    private final String code;
    private final String displayName;
    private final short minParticipants;
    private final short maxParticipants;
    private final boolean active;
}
