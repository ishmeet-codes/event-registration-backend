package com.registration.management.checkin.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCheckinResponseDTO {

    private int totalSubmitted;
    private int successCount;
    private int failureCount;

    @Builder.Default
    private List<CheckinResponseDTO> checkins = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
