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
            Role superAdminRole = roleRepository.findByRoleCode("SUPER_ADMIN").orElse(null);

            String adminEmail = "admin@registration.com";
            String userEmail = "user@registration.com";

            if (userRepository.findByEmail(adminEmail).isEmpty() && superAdminRole != null) {
                User admin = User.builder()
                        .fullName("Admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("admin123"))
                        .role(superAdminRole)
                        .active(true)
                        .build();

                userRepository.save(admin);
                System.out.println("Initialized admin user: " + adminEmail);
            }

            if (userRepository.findByEmail(userEmail).isEmpty()) {
                Role REGISTRATION_TEAM = roleRepository.findByRoleCode("REGISTRATION_TEAM")
                        .orElseThrow(() -> new IllegalStateException("REGISTRATION_TEAM role not found in database"));

                User user = User.builder()
                        .fullName("User")
                        .email(userEmail)
                        .password(passwordEncoder.encode("user123"))
                        .role(REGISTRATION_TEAM)
                        .active(true)
                        .build();

                    userRepository.save(user);
                    System.out.println("Initialized user: " + userEmail);
                }
            };
        }
    }
