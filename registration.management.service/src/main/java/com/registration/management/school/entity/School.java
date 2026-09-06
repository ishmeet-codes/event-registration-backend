package com.registration.management.school.entity;

import com.registration.management.auth.entities.User;
import com.registration.management.checkin.entity.Checkin;
import com.registration.management.registration.entity.Registration;

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
        "createdBy",
        "updatedBy",
        "schoolStaff",
        "registrations",
        "checkins"
})
@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "school_code", nullable = false, unique = true, length = 50)
    private String schoolCode;

    @NotBlank
    @Size(max = 150)
    @Column(name = "school_name", nullable = false, length = 150)
    private String schoolName;

    @Size(max = 120)
    @Column(name = "principal_name", length = 120)
    private String principalName;

    @Size(max = 50)
    @Column(name = "board", length = 50)
    private String board;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100)
    @Column(name = "district", length = 100)
    private String district;

    @Size(max = 100)
    @Column(name = "state", length = 100)
    private String state;

    @Pattern(regexp = "^[0-9]{6}$")
    @Column(name = "pincode", length = 10)
    private String pincode;

    @Pattern(regexp = "^[0-9]{10,15}$")
    @Column(name = "phone", length = 20)
    private String phone;

    @Email
    @Size(max = 120)
    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            foreignKey = @ForeignKey(name = "fk_schools_created_by")
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            foreignKey = @ForeignKey(name = "fk_schools_updated_by")
    )
    private User updatedBy;

    @OneToMany(
            mappedBy = "school",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<SchoolStaff> schoolStaff = new HashSet<>();

    @OneToMany(
            mappedBy = "school",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<Registration> registrations = new HashSet<>();

    @OneToMany(
            mappedBy = "school",
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

    public void addStaff(SchoolStaff staff) {
        schoolStaff.add(staff);
        staff.setSchool(this);
    }

    public void removeStaff(SchoolStaff staff) {
        schoolStaff.remove(staff);
        staff.setSchool(null);
    }

    public void addRegistration(Registration registration) {
        registrations.add(registration);
        registration.setSchool(this);
    }

    public void removeRegistration(Registration registration) {
        registrations.remove(registration);
        registration.setSchool(null);
    }

    public void addCheckin(Checkin checkin) {
        checkins.add(checkin);
        checkin.setSchool(this);
    }

    public void removeCheckin(Checkin checkin) {
        checkins.remove(checkin);
        checkin.setSchool(null);
    }
}