package com.registration.management.auth.service;

import com.registration.management.auth.dto.*;
import com.registration.management.auth.entities.PasswordResetToken;
import com.registration.management.auth.entities.RefreshToken;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.PasswordResetTokenRepository;
import com.registration.management.auth.repository.RefreshTokenRepository;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.auth.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = Logger.getLogger(AuthServiceImpl.class.getName());

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    @Value("${app.mail.reset-password-base-url}")
    private String resetPasswordBaseUrl;

    @Autowired private com.registration.management.auth.repository.RoleRepository roleRepository;

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // Resolve the requested role; fall back to PARTICIPANT if not found
        String requestedRoleCode = (request.getRoleCode() != null && !request.getRoleCode().isBlank())
                ? request.getRoleCode()
                : "PARTICIPANT";

        com.registration.management.auth.entities.Role role = roleRepository
                .findByRoleCode(requestedRoleCode)
                .orElseGet(() -> roleRepository.findByRoleCode("PARTICIPANT").orElse(null));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getUsername()) // Using username as full name for now
                .active(true)
                .role(role)
                .build();

        userRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = (User) auth.getPrincipal();

        // Update last login timestamp
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken  = jwtUtil.generateAccessToken(user.getEmail());
        String rawRefresh   = jwtUtil.generateRefreshToken();

        // Persist refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(rawRefresh)
                .expiresAt(LocalDateTime.now().plusNanos(JwtUtil.REFRESH_TOKEN_EXPIRY_MS * 1_000_000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefresh)
                .expiresIn(JwtUtil.ACCESS_TOKEN_EXPIRY_MS / 1000)
                .user(toProfile(user))
                .build();
    }

    @Override
    public AuthResponse refresh(TokenRefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        User user = stored.getUser();
        String newAccess = jwtUtil.generateAccessToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(stored.getToken())
                .expiresIn(JwtUtil.ACCESS_TOKEN_EXPIRY_MS / 1000)
                .user(toProfile(user))
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto me(User currentUser) {
        return toProfile(currentUser);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // Invalidate any previous unused tokens for this user
            passwordResetTokenRepository.findAllByUserAndUsedFalse(user)
                    .forEach(old -> {
                        old.setUsed(true);
                        passwordResetTokenRepository.save(old);
                    });

            String token = UUID.randomUUID().toString();

            PasswordResetToken prt = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(prt);

            String resetLink = resetPasswordBaseUrl + "/reset-password?token=" + token;
            log.info("[PASSWORD RESET] Sending reset email to: " + user.getEmail());
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token is invalid or has already been used");
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);

        // Revoke all refresh tokens for this user for security
        user.getRefreshTokens().forEach(rt -> rt.setRevoked(true));
    }

    @Override
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        // Re-fetch so we get the latest password hash
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private UserProfileDto toProfile(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().getRoleCode() : null)
                .permissions(user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> !a.startsWith("ROLE_"))
                        .collect(Collectors.toList()))
                .build();
    }
}
