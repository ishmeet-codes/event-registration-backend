package com.registration.management.registration.entity;

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
@EqualsAndHashCode(of = {"registration", "event"})
@ToString(exclude = {"registration", "event"})
@Entity
@Table(
        name = "registration_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_registration_event",
                        columnNames = {"registration_id", "event_id"}
                )
        }
)
public class RegistrationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "registration_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_registration_events_registration")
    )
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_registration_events_event")
    )
    private Event event;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
