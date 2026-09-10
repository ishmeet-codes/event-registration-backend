package com.registration.management.notification.entity;

import com.registration.management.auth.entities.User;
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
@ToString(exclude = "createdBy")
@Entity
@Table(name = "email_automations")
public class EmailAutomation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank
    @Column(name = "trigger_type", nullable = false, length = 100)
    private String triggerType;

    @NotNull
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "target_roles_csv", length = 255)
    private String targetRolesCsv;

    @Column(name = "conditions_json", columnDefinition = "TEXT")
    private String conditionsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", foreignKey = @ForeignKey(name = "fk_automations_created_by"))
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
