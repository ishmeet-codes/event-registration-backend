package com.registration.management.participant.entity;

import com.registration.management.event.entities.Event;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"participant", "event"})
@ToString(exclude = {"participant", "event"})
@Entity
@Table(
        name = "registration_participant_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_participant_event",
                        columnNames = {"participant_id", "event_id"}
                )
        }
)
public class ParticipantEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "participant_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_participant_events_participant")
    )
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_participant_events_event")
    )
    private Event event;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
