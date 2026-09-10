package com.registration.management.notification.sender;

import com.registration.management.notification.entity.Notification;
import com.registration.management.notification.entity.NotificationLog;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.repository.NotificationLogRepository;
import com.registration.management.notification.repository.NotificationRepository;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationChannelSender {

    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${app.mail.from:Acme <onboarding@resend.dev>}")
    private String fromAddress;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        String recipientEmail = notification.getRecipient() != null ? notification.getRecipient().getEmail() : null;
        if (recipientEmail == null || recipientEmail.isBlank()) {
            failNotification(notification, "Recipient email is missing or empty", recipientEmail);
            return;
        }

        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);

        try {
            if (resendApiKey == null || resendApiKey.isBlank()) {
                throw new IllegalStateException("Resend API key is not configured");
            }

            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                .from((fromAddress != null && !fromAddress.isBlank()) ? fromAddress : "Acme <onboarding@resend.dev>")
                .to(recipientEmail)
                .subject(notification.getTitle() != null ? notification.getTitle() : "Notification")
                .html(buildHtmlEmail(notification))
                .build();

            resend.emails().send(params);

            LocalDateTime now = java.time.LocalDateTime.now();
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(now);
            notification.setFailureReason(null);
            notificationRepository.save(notification);

            NotificationLog logEntry = NotificationLog.builder()
                    .notification(notification)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.SENT)
                    .recipientEmail(recipientEmail)
                    .attempts(notification.getRetryCount() + 1)
                    .sentAt(now)
                    .build();
            notificationLogRepository.save(logEntry);

            log.info("Email notification id={} sent successfully to {}", notification.getId(), recipientEmail);
        } catch (Exception ex) {
            log.error("Failed to send email notification id={} to {}: {}", notification.getId(), recipientEmail, ex.getMessage());
            failNotification(notification, ex.getMessage(), recipientEmail);
        }
    }

    private void failNotification(Notification notification, String reason, String recipientEmail) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailureReason(reason);
        notificationRepository.save(notification);

        NotificationLog logEntry = NotificationLog.builder()
                .notification(notification)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.FAILED)
                .recipientEmail(recipientEmail)
                .attempts(notification.getRetryCount() + 1)
                .failureReason(reason)
                .build();
        notificationLogRepository.save(logEntry);
    }

    private String buildHtmlEmail(Notification notification) {
        String title = notification.getTitle() != null ? notification.getTitle() : "Notification";
        String message = notification.getMessage() != null ? notification.getMessage() : "";
        String recipientName = (notification.getRecipient() != null && notification.getRecipient().getFullName() != null)
                ? notification.getRecipient().getFullName()
                : "Valued User";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="560" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;
                                      box-shadow:0 2px 12px rgba(0,0,0,.08);overflow:hidden;">
                          <tr>
                            <td style="background:linear-gradient(135deg,#3b82f6 0%%,#1d4ed8 100%%);
                                       padding:32px 40px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;">
                                %s
                              </h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:36px 40px 24px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;line-height:1.6;">
                                Hi <strong>%s</strong>,
                              </p>
                              <div style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;white-space:pre-wrap;">
                                %s
                              </div>
                            </td>
                          </tr>
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
                """.formatted(title, title, recipientName, message);
    }
}
