package com.registration.management.checkin.dto;

import com.registration.management.checkin.enums.CredentialType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialResponseDTO {

    private Long id;
    private CredentialType credentialType;
    private String token; // Opaque raw token for client display/scanning
    private String qrCodeDataUri; // Renderable Base64 data URI
    private Long participantId;
    private String participantName;
    private Long schoolStaffId;
    private String schoolStaffName;
    private Long schoolId;
    private String schoolName;
    private Long eventId;
    private String eventName;
    private Long registrationId;
    private boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
