package com.registration.management.auth.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetLink);

    /**
     * Sends a welcome email to a newly provisioned school-staff account,
     * prompting them to set their password before their first login.
     *
     * @param toEmail   the staff member's email address
     * @param fullName  the staff member's display name
     * @param setPasswordLink the one-time password-setup link (same reset flow)
     */
    void sendStaffWelcomeEmail(String toEmail, String fullName, String setPasswordLink);
}
