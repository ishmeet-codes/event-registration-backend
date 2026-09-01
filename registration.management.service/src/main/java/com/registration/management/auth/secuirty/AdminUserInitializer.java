package com.registration.management.auth.secuirty;

import com.registration.management.auth.entities.Role;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.RoleRepository;
import com.registration.management.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer {

    @Bean
    public CommandLineRunner init(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@registration.com";
            String userEmail = "user@registration.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                Role superAdminRole = roleRepository.findByRoleCode("SUPER_ADMIN")
                        .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not found in database"));

                User admin = User.builder()
                        .fullName("Admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("admin123"))
                        .role(superAdminRole)
                        .active(true)
                        .build();

                userRepository.save(admin);
                System.out.println("Initialized admin user: " + admin);
            }
            if (userRepository.findByEmail(userEmail).isEmpty()) {
                Role adminRole = roleRepository.findByRoleCode("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ADMIN role not found in database"));

                User user = User.builder()
                        .fullName("User")
                        .email(userEmail)
                        .password(passwordEncoder.encode("user123"))
                        .role(adminRole)
                        .active(true)
                        .build();

                userRepository.save(user);
                System.out.println("Initialized admin user: " + user);
            }
        };
    }
}