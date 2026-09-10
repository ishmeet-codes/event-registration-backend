package com.registration.management.notification.serviceImpl;

import com.registration.management.auth.entities.User;
import com.registration.management.notification.dto.CampaignRequestDTO;
import com.registration.management.notification.dto.CampaignResponseDTO;
import com.registration.management.notification.entity.EmailCampaign;
import com.registration.management.notification.entity.EmailCampaignRecipient;
import com.registration.management.notification.enums.CampaignStatus;
import com.registration.management.notification.enums.RecipientType;
import com.registration.management.notification.repository.EmailCampaignRecipientRepository;
import com.registration.management.notification.repository.EmailCampaignRepository;
import com.registration.management.notification.service.AudienceBuilderService;
import com.registration.management.notification.service.EmailCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCampaignServiceImpl implements EmailCampaignService {

    private final EmailCampaignRepository campaignRepository;
    private final EmailCampaignRecipientRepository recipientRepository;
    private final AudienceBuilderService audienceBuilderService;
    private final CampaignAsyncDispatcher campaignAsyncDispatcher;

    @Override
    @Transactional
    public CampaignResponseDTO createCampaign(CampaignRequestDTO request, User currentUser) {
        CampaignStatus status = (request.getScheduledAt() != null && request.getScheduledAt().isAfter(LocalDateTime.now()))
                ? CampaignStatus.SCHEDULED
                : CampaignStatus.PROCESSING;

        EmailCampaign campaign = EmailCampaign.builder()
                .name(request.getTitle())
                .description(request.getMessage())
                .templateId(request.getTemplateId())
                .status(status)
                .audienceType(request.getRecipientType() != null ? request.getRecipientType() : RecipientType.SCHOOL_STAFF)
                .scheduledAt(request.getScheduledAt())
                .startedAt(status == CampaignStatus.PROCESSING ? LocalDateTime.now() : null)
                .createdBy(currentUser)
                .build();

        List<EmailCampaignRecipient> recipientsList = audienceBuilderService.buildRecipientsForCampaign(
                campaign, request.getAudienceCriteria(), request.getCustomRecipients()
        );

        campaign.setTotalRecipients(recipientsList.size());
        int validCount = (int) recipientsList.stream().filter(r -> "VALID".equalsIgnoreCase(r.getStatus())).count();
        campaign.setValidRecipientsCount(validCount);
        campaign.setRecipients(recipientsList);

        EmailCampaign saved = campaignRepository.save(campaign);

        if (status == CampaignStatus.PROCESSING) {
            campaignAsyncDispatcher.dispatchCampaignAsync(saved.getId(), request.getTitle(), request.getMessage());
        }

        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CampaignResponseDTO> getCampaigns(int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return campaignRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponseDTO getCampaignById(Long id) {
        EmailCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id " + id));
        return mapToDTO(campaign);
    }

    @Override
    @Transactional
    public void cancelCampaign(Long id, User currentUser) {
        EmailCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id " + id));
        campaign.setStatus(CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public void deleteCampaign(Long id, User currentUser) {
        EmailCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id " + id));
        campaignRepository.delete(campaign);
    }

    @Override
    @Transactional
    public CampaignResponseDTO retryCampaign(Long id, User currentUser) {
        EmailCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id " + id));

        if (campaign.getRecipients() != null) {
            for (EmailCampaignRecipient recipient : campaign.getRecipients()) {
                if ("FAILED".equalsIgnoreCase(recipient.getStatus()) || "INVALID".equalsIgnoreCase(recipient.getStatus())) {
                    recipient.setStatus("VALID");
                    recipient.setErrorMessage(null);
                    recipient.setFailureReason(null);
                    recipient.setFailedAt(null);
                }
            }
        }

        int validCount = (campaign.getRecipients() != null)
                ? (int) campaign.getRecipients().stream().filter(r -> "VALID".equalsIgnoreCase(r.getStatus())).count()
                : 0;

        campaign.setValidRecipientsCount(validCount);
        campaign.setSentCount(0);
        campaign.setFailedCount(0);
        campaign.setDeliveredCount(0);
        campaign.setBouncedCount(0);
        campaign.setCompletedAt(null);
        campaign.setStartedAt(LocalDateTime.now());
        campaign.setStatus(CampaignStatus.PROCESSING);

        EmailCampaign saved = campaignRepository.save(campaign);

        campaignAsyncDispatcher.dispatchCampaignAsync(saved.getId(), saved.getName(), saved.getDescription());

        return mapToDTO(saved);
    }

    @Override
    public void sendTestEmail(Long campaignId, User currentUser) {
        if (currentUser != null && currentUser.getEmail() != null) {
            sendDirectTestEmail(currentUser.getEmail(), currentUser);
        }
    }

    @Override
    public void sendDirectTestEmail(String email, User currentUser) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Recipient email address is invalid: " + email);
        }
        String subject = "[TEST DISPATCH] Email Campaign Verification Sample";
        String body = """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;padding:20px;background:#f4f6f9;">
                  <div style="max-width:500px;margin:0 auto;background:#ffffff;padding:24px;border-radius:8px;border:1px solid #e2e8f0;">
                    <h2 style="color:#4f46e5;margin-top:0;">Campaign Safety Verification Test</h2>
                    <p style="color:#334155;">Hello,</p>
                    <p style="color:#334155;">This is a test email copy dispatched from the <strong>Email Campaign Management Hub</strong> to verify SMTP mail relay connectivity and template styling.</p>
                    <hr style="border:none;border-top:1px solid #e2e8f0;margin:20px 0;" />
                    <p style="font-size:12px;color:#64748b;margin-bottom:0;">Event Registration System &bull; Test Email Verification</p>
                  </div>
                </body>
                </html>
                """;
        try {
            campaignAsyncDispatcher.sendMimeEmail(email, subject, body);
        } catch (Exception ex) {
            String cleanError = campaignAsyncDispatcher.extractErrorMessage(ex);
            log.error("Direct test email failed to recipient {}. Root cause: {}", email, cleanError, ex);
            throw new RuntimeException(cleanError, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailCampaignRecipient> getCampaignRecipients(Long campaignId) {
        return recipientRepository.findByCampaignId(campaignId);
    }

    private CampaignResponseDTO mapToDTO(EmailCampaign c) {
        String lastError = null;
        if (c.getFailedCount() != null && c.getFailedCount() > 0) {
            java.util.Optional<EmailCampaignRecipient> failed = recipientRepository.findFirstByCampaignIdAndErrorMessageIsNotNull(c.getId());
            if (failed.isPresent()) {
                lastError = failed.get().getErrorMessage();
            }
        }

        return CampaignResponseDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .templateId(c.getTemplateId())
                .templateVersion(c.getTemplateVersion())
                .status(c.getStatus())
                .audienceType(c.getAudienceType())
                .totalRecipients(c.getTotalRecipients())
                .validRecipientsCount(c.getValidRecipientsCount())
                .sentCount(c.getSentCount())
                .failedCount(c.getFailedCount())
                .deliveredCount(c.getDeliveredCount())
                .bouncedCount(c.getBouncedCount())
                .scheduledAt(c.getScheduledAt())
                .startedAt(c.getStartedAt())
                .completedAt(c.getCompletedAt())
                .createdBy(c.getCreatedBy() != null ? c.getCreatedBy().getUsername() : "System Admin")
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .lastErrorMessage(lastError)
                .build();
    }
}

