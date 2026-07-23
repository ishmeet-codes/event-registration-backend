package com.registration.management.school.entities;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.StaffRole;
import com.registration.management.registration.entities.Checkin;
import com.registration.management.registration.entities.Registration;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@ToString(exclude = {
        "school",
        "user",
        "createdBy",
        "updatedBy",
        "registrations",
        "checkins"
})
@Entity
@Table(name = "school_staff")
public class SchoolStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "school_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_school_staff_school")
    )
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "fk_school_staff_user")
    )
    private User user;

    @NotBlank
    @Size(max = 120)
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Size(max = 100)
    @Column(name = "designation", length = 100)
    private String designation;

    @Pattern(regexp = "^[0-9]{10,15}$")
    @Column(name = "phone", length = 20)
    private String phone;

    @Email
    @Size(max = 120)
    @Column(name = "email", length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_role", nullable = false)
    private StaffRole staffRole;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            foreignKey = @ForeignKey(name = "fk_school_staff_created_by")
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            foreignKey = @ForeignKey(name = "fk_school_staff_updated_by")
    )
    private User updatedBy;

    @OneToMany(
            mappedBy = "createdByStaff",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<Registration> registrations = new HashSet<>();

    @OneToMany(
            mappedBy = "schoolStaff",
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

    public void addRegistration(Registration registration) {
        registrations.add(registration);
        registration.setCreatedByStaff(this);
    }

    public void removeRegistration(Registration registration) {
        registrations.remove(registration);
        registration.setCreatedByStaff(null);
    }

    public void addCheckin(Checkin checkin) {
        checkins.add(checkin);
        checkin.setSchoolStaff(this);
    }

    public void removeCheckin(Checkin checkin) {
        checkins.remove(checkin);
        checkin.setSchoolStaff(null);
    }
}