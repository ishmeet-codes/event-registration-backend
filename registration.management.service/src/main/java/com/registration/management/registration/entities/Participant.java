package com.registration.management.registration.entities;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
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
        "registration",
        "createdBy",
        "updatedBy",
        "checkins"
})
@Entity
@Table(name = "participants")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "registration_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_participants_registration")
    )
    private Registration registration;

    @NotBlank
    @Size(max = 120)
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Size(max = 20)
    @Column(name = "class_name", length = 20)
    private String className;

    @Past
    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Pattern(regexp = "^[0-9]{10,15}$")
    @Column(name = "guardian_phone", nullable = false, length = 20)
    private String guardianPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            foreignKey = @ForeignKey(name = "fk_participants_created_by")
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            foreignKey = @ForeignKey(name = "fk_participants_updated_by")
    )
    private User updatedBy;

    @OneToMany(
            mappedBy = "participant",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<Checkin> checkins = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addCheckin(Checkin checkin) {
        checkins.add(checkin);
        checkin.setParticipant(this);
    }

    public void removeCheckin(Checkin checkin) {
        checkins.remove(checkin);
        checkin.setParticipant(null);
    }
}