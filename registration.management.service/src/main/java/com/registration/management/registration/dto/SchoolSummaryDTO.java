package com.registration.management.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSummaryDTO {
    private Long id;
    private String schoolCode;
    private String schoolName;
}
