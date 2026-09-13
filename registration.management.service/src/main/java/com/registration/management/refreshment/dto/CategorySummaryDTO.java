package com.registration.management.refreshment.dto;

import com.registration.management.refreshment.enums.RecipientCategory;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryDTO {
    private RecipientCategory category;
    private Long eligible;
    private Long present;
    private Long distributed;
    private Long pending;
    private Double distributionRate;
}
