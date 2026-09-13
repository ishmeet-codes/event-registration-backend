package com.registration.management.refreshment.dto;

import com.registration.management.refreshment.enums.RecipientCategory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkGivenBulkRequestDTO {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Recipient category is required")
    private RecipientCategory recipientCategory;

    @NotEmpty(message = "Recipient IDs list cannot be empty")
    private List<Long> recipientIds;

    @Size(max = 500)
    private String remarks;
}
