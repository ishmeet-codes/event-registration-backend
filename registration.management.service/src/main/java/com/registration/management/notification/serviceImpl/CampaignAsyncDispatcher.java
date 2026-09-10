package com.registration.management.notification.serviceImpl;

import com.registration.management.notification.entity.EmailCampaign;
import com.registration.management.notification.entity.EmailCampaignRecipient;
import com.registration.management.notification.enums.CampaignStatus;
import com.registration.management.notification.repository.EmailCampaignRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignAsyncDispatcher {

    private final EmailCampaignRepository campaignRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@eventregistration.com}")
    private String fromAddress;

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

        for (EmailCampaignRecipient recipient : recipients) {
            if ("VALID".equalsIgnoreCase(recipient.getStatus())) {
                String personalizedBody = buildPersonalizedBody(templateToUse, recipient);
                boolean success = dispatchSingleEmail(recipient.getEmail(), emailSubject, personalizedBody);
                if (success) {
                    recipient.setSentAt(LocalDateTime.now());
                    recipient.setDeliveredAt(LocalDateTime.now());
                    sentCount++;
                } else {
                    recipient.setStatus("FAILED");
                    recipient.setErrorMessage("SMTP transport failed or mail relay unreachable");
                    failedCount++;
                }
            } else {
                failedCount++;
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

    public boolean dispatchSingleEmail(String toEmail, String subject, String bodyHtml) {
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String senderAddr = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress : "noreply@eventregistration.com";
            helper.setFrom(senderAddr);
            helper.setTo(toEmail);
            helper.setSubject(subject != null && !subject.isBlank() ? subject : "Campaign Notification");
            helper.setText(bodyHtml != null ? bodyHtml : "", true);
            mailSender.send(message);
            log.info("Campaign email successfully sent to {}", toEmail);
            return true;
        } catch (Exception ex) {
            log.error("Failed to transmit email to {}: {}", toEmail, ex.getMessage());
            return false;
        }
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
