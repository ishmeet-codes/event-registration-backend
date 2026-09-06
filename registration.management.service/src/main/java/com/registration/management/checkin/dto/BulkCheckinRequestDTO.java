package com.registration.management.checkin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCheckinRequestDTO {

    @NotEmpty(message = "Participant IDs list cannot be empty")
    private List<Long> participantIds;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
