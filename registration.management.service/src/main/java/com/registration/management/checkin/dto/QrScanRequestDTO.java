package com.registration.management.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrScanRequestDTO {

    @NotBlank(message = "QR Token is required")
    private String token;

    private Long eventId;
}
