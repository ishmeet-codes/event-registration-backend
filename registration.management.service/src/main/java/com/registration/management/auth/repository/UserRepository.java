package com.registration.management.auth.repository;

import com.registration.management.auth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Eagerly fetches the user's role and all its associated permissions in a
     * single JOIN so that {@code User#getAuthorities()} can be called safely
     * outside a transaction (e.g. inside {@code JwtAuthFilter}).
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.role r
            LEFT JOIN FETCH r.rolePermissions rp
            LEFT JOIN FETCH rp.permission
            WHERE u.email = :email
            """)
    Optional<User> findByEmail(@Param("email") String email);

    java.util.List<User> findByActiveTrue();

    java.util.List<User> findByRoleRoleCodeInAndActiveTrue(java.util.Collection<String> roleCodes);
}
