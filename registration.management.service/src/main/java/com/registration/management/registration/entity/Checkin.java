package com.registration.management.registration.entity;

import com.registration.management.auth.entities.User;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {
        "school",
        "participant",
        "schoolStaff",
        "checkedInBy"
})
@Entity
@Table(name = "checkins")
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "school_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_checkins_school")
    )
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "participant_id",
            foreignKey = @ForeignKey(name = "fk_checkins_participant")
    )
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "school_staff_id",
            foreignKey = @ForeignKey(name = "fk_checkins_school_staff")
    )
    private SchoolStaff schoolStaff;

    @Column(name = "present", nullable = false)
    private boolean present;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "checked_in_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_checkins_checked_in_by")
    )
    private User checkedInBy;

    @CreationTimestamp
    @Column(name = "checked_in_at", nullable = false, updatable = false)
    private LocalDateTime checkedInAt;

    @Size(max = 500)
    @Column(name = "remarks", length = 500)
    private String remarks;

    public boolean isParticipantCheckin() {
        return participant != null;
    }

    public boolean isStaffCheckin() {
        return schoolStaff != null;
    }
}