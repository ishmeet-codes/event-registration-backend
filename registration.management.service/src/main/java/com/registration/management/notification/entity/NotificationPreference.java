package com.registration.management.notification.entity;

import com.registration.management.auth.entities.User;
import jakarta.persistence.*;
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
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_notification_preferences_user")
    )
    private User user;

    @Builder.Default
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Builder.Default
    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = false;

    @Builder.Default
    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Builder.Default
    @Column(name = "event_enabled", nullable = false)
    private boolean eventEnabled = true;

    @Builder.Default
    @Column(name = "registration_enabled", nullable = false)
    private boolean registrationEnabled = true;

    @Builder.Default
    @Column(name = "checkin_enabled", nullable = false)
    private boolean checkinEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
