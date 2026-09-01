package com.registration.management.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StatusRequest {
    @NotNull private Boolean active;
}
