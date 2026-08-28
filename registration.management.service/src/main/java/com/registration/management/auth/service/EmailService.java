package com.registration.management.auth.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetLink);
}
