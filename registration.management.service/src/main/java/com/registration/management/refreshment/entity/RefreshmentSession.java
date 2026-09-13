package com.registration.management.refreshment.entity;

import com.registration.management.auth.entities.User;
import com.registration.management.event.entities.Event;
import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.refreshment.enums.TrackingType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"plan", "event", "assignments", "distributions", "countDistributions", "createdBy", "updatedBy"})
@Entity
@Table(name = "refreshment_sessions")
public class RefreshmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refreshment_sessions_plan"))
    private RefreshmentPlan plan;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(name = "fk_refreshment_sessions_event"))
    private Event event;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_category", nullable = false, length = 50)
    private RecipientCategory recipientCategory;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_type", nullable = false, length = 50)
    @Builder.Default
    private TrackingType trackingType = TrackingType.INDIVIDUAL;

    @Column(name = "expected_count")
    @Builder.Default
    private Integer expectedCount = 0;

    @Column(name = "count_distributed")
    @Builder.Default
    private Integer countDistributed = 0;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "require_checkin_presence", nullable = false)
    @Builder.Default
    private Boolean requireCheckinPresence = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshmentAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshmentDistribution> distributions = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GuestCountDistribution> countDistributions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_refreshment_sessions_created_by"))
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", foreignKey = @ForeignKey(name = "fk_refreshment_sessions_updated_by"))
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
