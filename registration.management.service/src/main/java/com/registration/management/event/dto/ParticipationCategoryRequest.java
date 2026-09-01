package com.registration.management.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ParticipationCategoryRequest {
    @NotBlank @Size(max = 50) private String code;
    @NotBlank @Size(max = 50) private String displayName;
    @NotNull @Positive private Short minParticipants;
    @NotNull @Positive private Short maxParticipants;
}
