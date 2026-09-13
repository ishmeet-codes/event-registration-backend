package com.registration.management.refreshment.entity;

import com.registration.management.auth.entities.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"session", "team", "createdBy"})
@Entity
@Table(
        name = "refreshment_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_refreshment_session_team",
                        columnNames = {"session_id", "team_id"}
                )
        }
)
public class RefreshmentAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refreshment_assignments_session"))
    private RefreshmentSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refreshment_assignments_team"))
    private RefreshmentTeam team;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_refreshment_assignments_created_by"))
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
