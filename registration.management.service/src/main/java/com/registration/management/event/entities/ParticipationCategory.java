package com.registration.management.event.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "events")
@Entity
@Table(name = "participation_categories")
public class ParticipationCategory {

    @Id
    @EqualsAndHashCode.Include
    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotBlank
    @Size(max = 50)
    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Positive
    @Column(name = "min_participants", nullable = false)
    private short minParticipants;

    @Positive
    @Column(name = "max_participants", nullable = false)
    private short maxParticipants;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(
            mappedBy = "participationCategory",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<Event> events = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addEvent(Event event) {
        events.add(event);
        event.setParticipationCategory(this);
    }

    public void removeEvent(Event event) {
        events.remove(event);
        event.setParticipationCategory(null);
    }
}