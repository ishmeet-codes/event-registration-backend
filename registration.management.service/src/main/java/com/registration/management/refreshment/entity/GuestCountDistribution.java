package com.registration.management.refreshment.entity;

import com.registration.management.auth.entities.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@ToString(exclude = {"session", "distributedBy"})
@Entity
@Table(name = "guest_count_distributions")
public class GuestCountDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, foreignKey = @ForeignKey(name = "fk_guest_count_dist_session"))
    private RefreshmentSession session;

    @NotNull
    @Min(1)
    @Column(name = "count_distributed", nullable = false)
    private Integer countDistributed;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distributed_by", nullable = false, foreignKey = @ForeignKey(name = "fk_guest_count_dist_by"))
    private User distributedBy;

    @NotNull
    @Column(name = "distributed_at", nullable = false)
    private LocalDateTime distributedAt;

    @Size(max = 500)
    @Column(name = "remarks", length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
