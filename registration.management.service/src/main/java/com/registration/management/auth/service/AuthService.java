package com.registration.management.auth.service;

import com.registration.management.auth.dto.*;
import com.registration.management.auth.entities.User;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(TokenRefreshRequest request);
    void logout(String refreshToken);
    UserProfileDto me(User currentUser);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(User currentUser, ChangePasswordRequest request);
}
