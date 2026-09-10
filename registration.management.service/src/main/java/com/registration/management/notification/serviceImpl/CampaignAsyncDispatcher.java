package com.registration.management.notification.serviceImpl;

import com.registration.management.notification.entity.EmailCampaign;
import com.registration.management.notification.entity.EmailCampaignRecipient;
import com.registration.management.notification.enums.CampaignStatus;
import com.registration.management.notification.repository.EmailCampaignRepository;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignAsyncDispatcher {

    private final EmailCampaignRepository campaignRepository;

    @Value("${app.mail.from:Acme <onboarding@resend.dev>}")
    private String fromAddress;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Async
    @Transactional
    public void dispatchCampaignAsync(Long campaignId, String subject, String bodyTemplate) {
        log.info("Starting async email campaign dispatch for campaign ID: {}", campaignId);
        EmailCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            log.error("Async dispatch failed: Campaign ID {} not found", campaignId);
            return;
        }

        campaign.setStatus(CampaignStatus.PROCESSING);
        if (campaign.getStartedAt() == null) {
            campaign.setStartedAt(LocalDateTime.now());
        }
        campaignRepository.save(campaign);

        List<EmailCampaignRecipient> recipients = campaign.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            log.warn("Campaign ID {} has no recipients to dispatch", campaignId);
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaign.setCompletedAt(LocalDateTime.now());
            campaignRepository.save(campaign);
            return;
        }

        String emailSubject = (subject != null && !subject.isBlank()) ? subject : (campaign.getName() != null ? campaign.getName() : "Notification");
        String templateToUse = (bodyTemplate != null && !bodyTemplate.isBlank()) 
                ? bodyTemplate 
                : "Dear {{full_name}},\n\nThis is an official campaign notification from Event Registration Management System.";

        int sentCount = 0;
        int failedCount = 0;

        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("Resend API key is missing. Aborting campaign.");
            failAllRecipients(recipients, "Resend API key is not configured");
            failedCount = recipients.size();
        } else {
            Resend resend = new Resend(resendApiKey);
            String senderAddr = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress : "Acme <onboarding@resend.dev>";

            List<CreateEmailOptions> batchOptions = new ArrayList<>();
            List<EmailCampaignRecipient> validRecipients = new ArrayList<>();

            for (EmailCampaignRecipient recipient : recipients) {
                if ("VALID".equalsIgnoreCase(recipient.getStatus())) {
                    String personalizedBody = buildPersonalizedBody(templateToUse, recipient);
                    batchOptions.add(CreateEmailOptions.builder()
                            .from(senderAddr)
                            .to(recipient.getEmail())
                            .subject(emailSubject)
                            .html(personalizedBody)
                            .build());
                    validRecipients.add(recipient);
                } else {
                    recipient.setStatus("FAILED");
                    recipient.setFailedAt(LocalDateTime.now());
                    if (recipient.getErrorMessage() == null || recipient.getErrorMessage().isBlank()) {
                        recipient.setErrorMessage("Recipient marked as INVALID before dispatch");
                        recipient.setFailureReason("Recipient marked as INVALID before dispatch");
                    }
                    failedCount++;
                }
            }

            // Resend supports up to 100 emails per batch request
            int batchSize = 100;
            for (int i = 0; i < batchOptions.size(); i += batchSize) {
                int end = Math.min(i + batchSize, batchOptions.size());
                List<CreateEmailOptions> chunk = batchOptions.subList(i, end);
                List<EmailCampaignRecipient> chunkRecipients = validRecipients.subList(i, end);

                try {
                    resend.batch().send(chunk);
                    for (EmailCampaignRecipient recipient : chunkRecipients) {
                        recipient.setSentAt(LocalDateTime.now());
                        recipient.setDeliveredAt(LocalDateTime.now());
                        recipient.setStatus("DELIVERED");
                        recipient.setErrorMessage(null);
                        recipient.setFailureReason(null);
                        sentCount++;
                    }
                } catch (Exception ex) {
                    String errorReason = extractErrorMessage(ex);
                    log.error("Failed to transmit email batch. Root cause: {}", errorReason);
                    for (EmailCampaignRecipient recipient : chunkRecipients) {
                        recipient.setStatus("FAILED");
                        recipient.setFailedAt(LocalDateTime.now());
                        recipient.setErrorMessage(errorReason);
                        recipient.setFailureReason(errorReason);
                        failedCount++;
                    }
                }
            }
        }

        campaign.setSentCount(sentCount);
        campaign.setDeliveredCount(sentCount);
        campaign.setFailedCount(failedCount);
        campaign.setCompletedAt(LocalDateTime.now());
        campaign.setStatus((sentCount > 0 || failedCount == 0) ? CampaignStatus.COMPLETED : CampaignStatus.FAILED);

        campaignRepository.save(campaign);
        log.info("Completed async campaign dispatch for campaign ID: {}. Sent: {}, Failed: {}", campaignId, sentCount, failedCount);
    }

    private void failAllRecipients(List<EmailCampaignRecipient> recipients, String reason) {
        for (EmailCampaignRecipient recipient : recipients) {
            if ("VALID".equalsIgnoreCase(recipient.getStatus())) {
                recipient.setStatus("FAILED");
                recipient.setFailedAt(LocalDateTime.now());
                recipient.setErrorMessage(reason);
                recipient.setFailureReason(reason);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    @Transactional
    public void processScheduledCampaigns() {
        List<EmailCampaign> dueCampaigns = campaignRepository.findDueScheduledCampaigns(LocalDateTime.now());
        if (dueCampaigns.isEmpty()) {
            return;
        }
        for (EmailCampaign campaign : dueCampaigns) {
            log.info("Triggering scheduled campaign ID: {}", campaign.getId());
            campaign.setStatus(CampaignStatus.PROCESSING);
            campaignRepository.save(campaign);
            dispatchCampaignAsync(campaign.getId(), campaign.getName(), campaign.getDescription());
        }
    }

    public void sendMimeEmail(String toEmail, String subject, String bodyHtml) throws Exception {
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            throw new IllegalArgumentException("Recipient email is empty or invalid: " + toEmail);
        }
        
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("Resend API key is not configured. Please set resend.api.key in your properties.");
        }

        String senderAddr = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress : "Acme <onboarding@resend.dev>";
        
        Resend resend = new Resend(resendApiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(senderAddr)
                .to(toEmail)
                .subject(subject != null && !subject.isBlank() ? subject : "Campaign Notification")
                .html(bodyHtml != null ? bodyHtml : "")
                .build();

        resend.emails().send(params);
        log.info("Campaign email successfully sent to {} via Resend", toEmail);
    }

    public boolean dispatchSingleEmail(String toEmail, String subject, String bodyHtml) {
        try {
            sendMimeEmail(toEmail, subject, bodyHtml);
            return true;
        } catch (Exception ex) {
            log.error("Failed to transmit single email to {}. Root cause: {}", toEmail, extractErrorMessage(ex), ex);
            return false;
        }
    }

    public String extractErrorMessage(Throwable ex) {
        if (ex == null) return "Unknown mail error";
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String rootType = root.getClass().getSimpleName();
        String rootMsg = root.getMessage();
        if (rootMsg == null || rootMsg.isBlank()) {
            rootMsg = ex.getMessage();
        }
        if (rootMsg == null || rootMsg.isBlank()) {
            return rootType;
        }
        return rootType + ": " + rootMsg;
    }

    public String buildPersonalizedBody(String template, EmailCampaignRecipient recipient) {
        if (template == null) return "";
        String name = recipient.getRecipientName() != null ? recipient.getRecipientName() : "Valued Recipient";
        String school = recipient.getSchoolName() != null ? recipient.getSchoolName() : "N/A";
        String email = recipient.getEmail() != null ? recipient.getEmail() : "";

        String result = template;
        result = result.replace("{{full_name}}", name);
        result = result.replace("{{first_name}}", name.split(" ")[0]);
        result = result.replace("{{email}}", email);
        result = result.replace("{{school_name}}", school);
        result = result.replace("{{organization_name}}", "GNE Technical Institute");
        return result.replace("\n", "<br/>");
    }
}
