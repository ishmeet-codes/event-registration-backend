package com.registration.management.checkin.entity;

import com.registration.management.checkin.enums.CredentialType;
import com.registration.management.event.entities.Event;
import com.registration.management.participant.entity.Participant;
import com.registration.management.registration.entity.Registration;
import com.registration.management.school.entity.SchoolStaff;

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
@ToString(exclude = {
        "participant",
        "schoolStaff",
        "registration",
        "event"
})
@Entity
@Table(
        name = "checkin_credentials",
        indexes = {
                @Index(name = "idx_checkin_credentials_token_hash", columnList = "token_hash"),
                @Index(name = "idx_checkin_credentials_participant", columnList = "participant_id"),
                @Index(name = "idx_checkin_credentials_staff", columnList = "school_staff_id"),
                @Index(name = "idx_checkin_credentials_registration", columnList = "registration_id"),
                @Index(name = "idx_checkin_credentials_event", columnList = "event_id"),
                @Index(name = "idx_checkin_credentials_active", columnList = "active")
        }
)
public class CheckInCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 30)
    private CredentialType credentialType;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "participant_id",
            foreignKey = @ForeignKey(name = "fk_credentials_participant")
    )
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "school_staff_id",
            foreignKey = @ForeignKey(name = "fk_credentials_staff")
    )
    private SchoolStaff schoolStaff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "registration_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_credentials_registration")
    )
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_credentials_event")
    )
    private Event event;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
