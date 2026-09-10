package com.registration.management.notification.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "campaign")
@Entity
@Table(name = "email_campaign_recipients")
public class EmailCampaignRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, foreignKey = @ForeignKey(name = "fk_campaign_recipients_campaign"))
    private EmailCampaign campaign;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "school_name", length = 255)
    private String schoolName;

    @Column(name = "recipient_type", length = 50)
    private String recipientType;

    @Column(name = "person_id")
    private Long personId;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "school_staff_id")
    private Long schoolStaffId;

    @Column(name = "participant_id")
    private Long participantId;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "VALID";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
