package com.registration.management.registration.entity;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.participant.entity.Participant;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {
        "school",
        "createdByStaff",
        "createdBy",
        "updatedBy",
        "registrationEvents",
        "participants"
})
@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "school_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_registrations_school")
    )
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_staff_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_registrations_created_by_staff")
    )
    private SchoolStaff createdByStaff;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "registration_status", nullable = false)
    private RegistrationStatus status;

    @Size(max = 500)
    @Column(name = "remarks", length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            foreignKey = @ForeignKey(name = "fk_registrations_created_by")
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            foreignKey = @ForeignKey(name = "fk_registrations_updated_by")
    )
    private User updatedBy;

    @OneToMany(
            mappedBy = "registration",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<RegistrationEvent> registrationEvents = new HashSet<>();

    @OneToMany(
            mappedBy = "registration",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<Participant> participants = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addEvent(Event event) {
        RegistrationEvent re = RegistrationEvent.builder()
                .registration(this)
                .event(event)
                .build();
        registrationEvents.add(re);
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
        participant.setRegistration(this);
    }

    public void removeParticipant(Participant participant) {
        participants.remove(participant);
        participant.setRegistration(null);
    }
}