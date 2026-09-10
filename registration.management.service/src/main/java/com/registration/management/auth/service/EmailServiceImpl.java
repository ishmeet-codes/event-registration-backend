package com.registration.management.auth.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.resend.core.exception.ResendException;

import java.util.logging.Logger;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = Logger.getLogger(EmailServiceImpl.class.getName());

    @Value("${app.mail.from:Acme <onboarding@resend.dev>}")
    private String fromAddress;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    // ─── Password Reset ───────────────────────────────────────────────────────

    /**
     * Sends a styled HTML email containing the password-reset link.
     * Runs asynchronously so the HTTP response is not held up by SMTP latency.
     */
    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            sendWithResend(toEmail, "Reset your password", buildResetEmailBody(resetLink));
            log.info("[EmailService] Password reset email sent to: " + toEmail);
        } catch (Exception e) {
            log.severe("[EmailService] Failed to send password reset email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String buildResetEmailBody(String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Reset your password</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="560" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;
                                      box-shadow:0 2px 12px rgba(0,0,0,.08);overflow:hidden;">
                
                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#4f46e5 0%%,#7c3aed 100%%);
                                       padding:36px 40px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;
                                         letter-spacing:-0.5px;">
                                Password Reset Request
                              </h1>
                            </td>
                          </tr>
                
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 24px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;line-height:1.6;">
                                Hi there,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                We received a request to reset the password for your account.
                                Click the button below to choose a new password.
                                This link is valid for <strong>1 hour</strong>.
                              </p>
                
                              <!-- CTA Button -->
                              <table cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td align="center" style="padding:8px 0 32px;">
                                    <a href="%s"
                                       style="display:inline-block;background:linear-gradient(135deg,#4f46e5 0%%,#7c3aed 100%%);
                                              color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;
                                              padding:14px 36px;border-radius:8px;
                                              letter-spacing:0.3px;">
                                      Reset Password
                                    </a>
                                  </td>
                                </tr>
                              </table>
                
                              <p style="margin:0 0 8px;color:#6b7280;font-size:13px;line-height:1.6;">
                                Or copy and paste this URL into your browser:
                              </p>
                              <p style="margin:0 0 24px;word-break:break-all;">
                                <a href="%s" style="color:#4f46e5;font-size:13px;">%s</a>
                              </p>
                
                              <p style="margin:0;color:#6b7280;font-size:13px;line-height:1.6;">
                                If you did not request a password reset, you can safely ignore this email.
                                Your password will not change.
                              </p>
                            </td>
                          </tr>
                
                          <!-- Footer -->
                          <tr>
                            <td style="background:#f9fafb;padding:20px 40px;border-top:1px solid #e5e7eb;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;text-align:center;">
                                &copy; 2026 Registration Management System. All rights reserved.
                              </p>
                            </td>
                          </tr>
                
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(resetLink, resetLink, resetLink);
    }

    // ─── Staff Welcome ────────────────────────────────────────────────────────

    /**
     * Sends a welcome email to a newly provisioned school-staff account so the
     * teacher can set their own password before their first login.
     * Runs asynchronously so the HTTP response is not blocked by SMTP latency.
     */
    @Async
    @Override
    public void sendStaffWelcomeEmail(String toEmail, String fullName, String setPasswordLink) {
        try {
            sendWithResend(toEmail, "Welcome — Set your password to get started", buildWelcomeEmailBody(fullName, setPasswordLink));
            log.info("[EmailService] Staff welcome email sent to: " + toEmail);
        } catch (Exception e) {
            log.severe("[EmailService] Failed to send staff welcome email to " + toEmail + ": " + e.getMessage());
            // Do not propagate — staff record is already saved; teacher can use Forgot Password later
        }
    }

    private String buildWelcomeEmailBody(String fullName, String setPasswordLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Welcome — Set your password</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="560" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;
                                      box-shadow:0 2px 12px rgba(0,0,0,.08);overflow:hidden;">
                
                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#4f46e5 0%%,#7c3aed 100%%);
                                       padding:36px 40px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;
                                         letter-spacing:-0.5px;">
                                Welcome to the System!
                              </h1>
                            </td>
                          </tr>
                
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 24px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;line-height:1.6;">
                                Hi <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                An account has been created for you as a school staff member.
                                Click the button below to set your password and activate your account.
                                This link is valid for <strong>1 hour</strong>.
                              </p>
                
                              <!-- CTA Button -->
                              <table cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td align="center" style="padding:8px 0 32px;">
                                    <a href="%s"
                                       style="display:inline-block;background:linear-gradient(135deg,#4f46e5 0%%,#7c3aed 100%%);
                                              color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;
                                              padding:14px 36px;border-radius:8px;
                                              letter-spacing:0.3px;">
                                      Set My Password
                                    </a>
                                  </td>
                                </tr>
                              </table>
                
                              <p style="margin:0 0 8px;color:#6b7280;font-size:13px;line-height:1.6;">
                                Or copy and paste this URL into your browser:
                              </p>
                              <p style="margin:0 0 24px;word-break:break-all;">
                                <a href="%s" style="color:#4f46e5;font-size:13px;">%s</a>
                              </p>
                
                              <p style="margin:0;color:#6b7280;font-size:13px;line-height:1.6;">
                                If you believe this email was sent by mistake, please contact your school administrator.
                              </p>
                            </td>
                          </tr>
                
                          <!-- Footer -->
                          <tr>
                            <td style="background:#f9fafb;padding:20px 40px;border-top:1px solid #e5e7eb;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;text-align:center;">
                                &copy; 2026 Registration Management System. All rights reserved.
                              </p>
                            </td>
                          </tr>
                
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(fullName, setPasswordLink, setPasswordLink, setPasswordLink);
    }

    // ─── Approval QR Email ───────────────────────────────────────────────────

    @Async
    @Override
    public void sendApprovalQrEmail(String toEmail, String personName, String personType, String schoolName, String eventName, String qrCodeDataUri) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        try {
            sendWithResend(toEmail, "Registration Approved — Check-in QR Code for " + eventName, buildApprovalQrEmailBody(personName, personType, schoolName, eventName, qrCodeDataUri));
            log.info("[EmailService] Approval QR email sent to: " + toEmail + " for event: " + eventName);
        } catch (Exception e) {
            log.severe("[EmailService] Failed to send approval QR email to " + toEmail + ": " + e.getMessage());
        }
    }

    private void sendWithResend(String to, String subject, String html) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("Resend API key is not configured");
        }
        
        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from((fromAddress != null && !fromAddress.isBlank()) ? fromAddress : "Acme <onboarding@resend.dev>")
                .to(to)
                .subject(subject)
                .html(html)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            log.severe("[EmailService] Resend API Error: " + e.getMessage());
            throw new RuntimeException("Resend API Error: " + e.getMessage(), e);
        }
    }

    private String buildApprovalQrEmailBody(String personName, String personType, String schoolName, String eventName, String qrCodeDataUri) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Registration Approved — Your QR Code</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="560" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;
                                      box-shadow:0 2px 12px rgba(0,0,0,.08);overflow:hidden;">
                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#059669 0%%,#10b981 100%%);
                                       padding:36px 40px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;">
                                Registration Approved!
                              </h1>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 24px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;line-height:1.6;">
                                Dear <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                Your registration for <strong>%s</strong> has been officially approved.
                              </p>
                              <table cellpadding="12" cellspacing="0" width="100%%" style="background:#f9fafb;border-radius:8px;margin-bottom:24px;">
                                <tr>
                                  <td style="color:#6b7280;font-size:13px;">Role / Category:</td>
                                  <td style="color:#111827;font-weight:600;font-size:13px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="color:#6b7280;font-size:13px;">School:</td>
                                  <td style="color:#111827;font-weight:600;font-size:13px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="color:#6b7280;font-size:13px;">Event:</td>
                                  <td style="color:#111827;font-weight:600;font-size:13px;">%s</td>
                                </tr>
                              </table>
                              
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;text-align:center;font-weight:600;">
                                Your Check-in QR Code
                              </p>
                              <div style="text-align:center;margin-bottom:24px;">
                                <img src="%s" alt="Check-in QR Code" width="220" height="220" style="border:4px solid #10b981;border-radius:12px;padding:8px;background:#ffffff;" />
                              </div>
                              <p style="margin:0;color:#6b7280;font-size:13px;line-height:1.6;text-align:center;">
                                Please present this QR code on your phone or bring a printed copy at the event check-in desk.
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="background:#f9fafb;padding:20px 40px;border-top:1px solid #e5e7eb;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;text-align:center;">
                                &copy; 2026 Event Registration Management System. All rights reserved.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(personName, eventName, personType, schoolName, eventName, qrCodeDataUri);
    }
}
