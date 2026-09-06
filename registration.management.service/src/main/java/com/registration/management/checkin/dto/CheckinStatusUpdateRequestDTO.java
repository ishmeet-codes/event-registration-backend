package com.registration.management.checkin.dto;

import com.registration.management.checkin.enums.CheckinStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinStatusUpdateRequestDTO {

    @NotNull(message = "Status is required")
    private CheckinStatus status;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
