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
@ToString(exclude = {"team", "user"})
@Entity
@Table(
        name = "refreshment_team_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_refreshment_team_member",
                        columnNames = {"team_id", "user_id"}
                )
        }
)
public class RefreshmentTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refreshment_team_members_team"))
    private RefreshmentTeam team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refreshment_team_members_user"))
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
