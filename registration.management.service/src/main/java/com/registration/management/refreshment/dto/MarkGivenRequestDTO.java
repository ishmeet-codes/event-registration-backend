package com.registration.management.refreshment.dto;

import com.registration.management.refreshment.enums.RecipientCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkGivenRequestDTO {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Recipient category is required")
    private RecipientCategory recipientCategory;

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;

    @Size(max = 500)
    private String remarks;
}
