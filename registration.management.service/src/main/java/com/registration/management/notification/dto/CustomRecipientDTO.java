package com.registration.management.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomRecipientDTO {
    private String id;

    @NotBlank
    private String email;

    private String name;
    private String school;
    private String status;
    private String errorMessage;
}
