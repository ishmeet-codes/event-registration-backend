package com.registration.management.refreshment.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshmentTeamResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Long leaderId;
    private String leaderName;
    private Boolean active;
    private List<TeamMemberDTO> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberDTO {
        private Long userId;
        private String fullName;
        private String email;
    }
}
