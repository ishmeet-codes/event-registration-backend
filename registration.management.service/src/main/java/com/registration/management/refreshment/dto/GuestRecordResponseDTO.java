package com.registration.management.refreshment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestRecordResponseDTO {
    private Long id;
    private Long planId;
    private String name;
    private String designation;
    private String organization;
    private String contactNumber;
    private String remarks;
    private Boolean active;
    private LocalDateTime createdAt;
}
