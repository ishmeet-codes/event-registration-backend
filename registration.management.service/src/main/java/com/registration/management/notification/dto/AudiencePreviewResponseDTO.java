package com.registration.management.notification.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudiencePreviewResponseDTO {

    private int total;
    private int validEmailsCount;
    private int invalidEmailsCount;
    private List<RecipientPreviewItem> recipientsPreview;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipientPreviewItem {
        private String name;
        private String email;
        private String school;
        private String role;
    }
}
