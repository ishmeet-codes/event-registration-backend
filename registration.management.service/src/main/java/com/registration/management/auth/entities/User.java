package com.registration.management.auth.entities;

import com.registration.management.checkin.entity.Checkin;
import com.registration.management.school.entity.SchoolStaff;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {
        "role",
        "createdBy",
        "updatedBy",
        "createdUsers",
        "updatedUsers",
        "refreshTokens",
        "schoolStaffMembers",
        "checkinsPerformed"
})
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "role_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_role")
    )
    private Role role;

    @Email
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> createdUsers = new HashSet<>();

    @OneToMany(mappedBy = "updatedBy", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> updatedUsers = new HashSet<>();

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<SchoolStaff> schoolStaffMembers = new HashSet<>();

    @OneToMany(mappedBy = "checkedInBy", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Checkin> checkinsPerformed = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add the role itself as ROLE_<CODE> (used with hasRole() / hasAnyRole())
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));

            // Add each permission code the role carries (used with hasAuthority())
            if (role.getRolePermissions() != null) {
                for (RolePermission rp : role.getRolePermissions()) {
                    if (rp.getPermission() != null) {
                        authorities.add(new SimpleGrantedAuthority(rp.getPermission().getPermissionCode()));
                    }
                }
            }
        }

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    public boolean isEnabled() {
        return active;
    }

    public String getRoleName() {
        return role != null ? role.getRoleName() : null;
    }
}