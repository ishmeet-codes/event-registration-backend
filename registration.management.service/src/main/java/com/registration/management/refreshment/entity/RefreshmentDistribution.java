package com.registration.management.refreshment.entity;

import com.registration.management.auth.entities.User;
import com.registration.management.participant.entity.Participant;
import com.registration.management.refreshment.enums.DistributionStatus;
import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.school.entity.SchoolStaff;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"session", "participant", "schoolStaff", "ocMember", "guestRecord", "distributedBy", "correctedBy"})
@Entity
@Table(name = "refreshment_distributions")
public class RefreshmentDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ref_dist_session"))
    private RefreshmentSession session;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_category", nullable = false, length = 50)
    private RecipientCategory recipientCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", foreignKey = @ForeignKey(name = "fk_ref_dist_participant"))
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_staff_id", foreignKey = @ForeignKey(name = "fk_ref_dist_staff"))
    private SchoolStaff schoolStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oc_member_id", foreignKey = @ForeignKey(name = "fk_ref_dist_oc"))
    private User ocMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", foreignKey = @ForeignKey(name = "fk_ref_dist_guest"))
    private GuestRecord guestRecord;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DistributionStatus status = DistributionStatus.GIVEN;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distributed_by", nullable = false, foreignKey = @ForeignKey(name = "fk_ref_dist_distributed_by"))
    private User distributedBy;

    @NotNull
    @Column(name = "distributed_at", nullable = false)
    private LocalDateTime distributedAt;

    @Size(max = 500)
    @Column(name = "remarks", length = 500)
    private String remarks;

    @Size(max = 500)
    @Column(name = "correction_reason", length = 500)
    private String correctionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by", foreignKey = @ForeignKey(name = "fk_ref_dist_corrected_by"))
    private User correctedBy;

    @Column(name = "corrected_at")
    private LocalDateTime correctedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
