package com.registration.management.checkin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QrScanResponseDTO {

    private boolean success;
    private String code;
    private String message;
    private Long checkInId;
    private String personName;
    private String personType;
    private String schoolName;
    private String eventName;
    private String checkInMethod;
    private LocalDateTime checkedInAt;
    private CheckedInByDTO checkedInBy;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckedInByDTO {
        private Long id;
        private String name;
        private String email;
    }
}
