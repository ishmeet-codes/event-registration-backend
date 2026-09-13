package com.registration.management.refreshment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshmentTeamRequestDTO {

    @NotBlank(message = "Team name is required")
    @Size(max = 100)
    private String name;

    private String description;

    private Long leaderId;

    private List<Long> memberUserIds;

    @Builder.Default
    private Boolean active = true;
}
