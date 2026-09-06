package com.registration.management.checkin.entity;

import com.registration.management.auth.entities.User;
import com.registration.management.checkin.enums.CheckinStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.participant.entity.Participant;
import com.registration.management.registration.entity.Registration;
import com.registration.management.school.entity.School;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
        "event",
        "registration",
        "school",
        "checkedInBy",
        "checkedOutBy"
})
@Entity
@Table(
        name = "checkins",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_checkins_participant",
                        columnNames = {"participant_id"}
                )
        }
)
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "participant_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_checkins_participant")
    )
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_checkins_event")
    )
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "registration_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_checkins_registration")
    )
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "school_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_checkins_school")
    )
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CheckinStatus status = CheckinStatus.CHECKED_IN;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "checked_in_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_checkins_checked_in_by")
    )
    private User checkedInBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "checked_out_by",
            foreignKey = @ForeignKey(name = "fk_checkins_checked_out_by")
    )
    private User checkedOutBy;

    @Size(max = 500)
    @Column(name = "remarks", length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
