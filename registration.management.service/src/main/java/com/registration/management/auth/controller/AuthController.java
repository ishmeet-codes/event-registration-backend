package com.registration.management.auth.controller;

import com.registration.management.auth.dto.*;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.registration.management.auth.service.CookiesService;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CookiesService cookiesService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.ok(Map.of("message", "User registered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        ResponseCookie cookie = cookiesService.generateRefreshTokenCookie(response.getRefreshToken());
        response.setRefreshToken(null);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "${app.cookie.refresh-token-name:refreshToken}", required = true) String refreshToken) {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(refreshToken);
        AuthResponse response = authService.refresh(request);
        
        ResponseCookie cookie = cookiesService.generateRefreshTokenCookie(response.getRefreshToken());
        response.setRefreshToken(null);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "${app.cookie.refresh-token-name:refreshToken}", required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            authService.logout(refreshToken);
        }
        
        ResponseCookie cleanCookie = cookiesService.getCleanRefreshTokenCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .build();
    }


    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.me(currentUser));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "If that email is registered you will receive a reset link shortly."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }

    @PostMapping("/password/change")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}
