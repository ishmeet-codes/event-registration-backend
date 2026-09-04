package com.registration.management.event.entities;

import com.registration.management.auth.entities.User;
import com.registration.management.registration.entity.Registration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
        "registrations"
})
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "participation_category_code",
            referencedColumnName = "code",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_events_participation_category")
    )
    private ParticipationCategory participationCategory;

    @NotBlank
    @Size(max = 150)
    @Column(name = "event_name", nullable = false, length = 150)
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 120)
    @Column(name = "venue", length = 120)
    private String venue;

    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Positive
    @Column(name = "max_registrations", nullable = false)
    private Integer maxRegistrations;

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
    private Set<Registration> registrations = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addRegistration(Registration registration) {
        registrations.add(registration);
        registration.setEvent(this);
    }

    public void removeRegistration(Registration registration) {
        registrations.remove(registration);
        registration.setEvent(null);
    }
}