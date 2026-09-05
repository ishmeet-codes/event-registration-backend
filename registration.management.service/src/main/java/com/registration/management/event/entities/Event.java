package com.registration.management.event.entities;

import com.registration.management.auth.entities.User;
import com.registration.management.registration.entity.RegistrationEvent;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {
        "participationCategory",
        "createdBy",
        "updatedBy",
        "registrationEvents"
})
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "participation_category_code",
            referencedColumnName = "code",
            foreignKey = @ForeignKey(name = "fk_events_participation_category")
    )
    private ParticipationCategory participationCategory;

    @NotBlank
    @Size(max = 150)
    @Column(name = "event_name", nullable = false, length = 150)
    private String eventName;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @Size(max = 200)
    @Column(name = "venue", length = 200)
    private String venue;

    @NotNull
    @Future
    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Min(1)
    @Column(name = "max_registrations", nullable = false)
    private int maxRegistrations;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            foreignKey = @ForeignKey(name = "fk_events_created_by")
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            foreignKey = @ForeignKey(name = "fk_events_updated_by")
    )
    private User updatedBy;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<RegistrationEvent> registrationEvents = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}