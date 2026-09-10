package com.registration.management.notification.entity;

import com.registration.management.auth.entities.User;
import com.registration.management.notification.enums.CampaignStatus;
import com.registration.management.notification.enums.RecipientType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"createdBy", "recipients"})
@Entity
@Table(name = "email_campaigns")
public class EmailCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "template_id")
    private Long templateId;

    @Builder.Default
    @Column(name = "template_version", nullable = false)
    private Integer templateVersion = 1;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 50)
    private RecipientType audienceType = RecipientType.SCHOOL_STAFF;

    @Column(name = "audience_criteria_json", columnDefinition = "TEXT")
    private String audienceCriteriaJson;

    @Builder.Default
    @Column(name = "total_recipients", nullable = false)
    private Integer totalRecipients = 0;

    @Builder.Default
    @Column(name = "valid_recipients_count", nullable = false)
    private Integer validRecipientsCount = 0;

    @Builder.Default
    @Column(name = "sent_count", nullable = false)
    private Integer sentCount = 0;

    @Builder.Default
    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    @Builder.Default
    @Column(name = "delivered_count", nullable = false)
    private Integer deliveredCount = 0;

    @Builder.Default
    @Column(name = "bounced_count", nullable = false)
    private Integer bouncedCount = 0;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", foreignKey = @ForeignKey(name = "fk_campaigns_created_by"))
    private User createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailCampaignRecipient> recipients = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
