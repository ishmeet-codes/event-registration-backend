package com.registration.management.notification.repository;

import com.registration.management.notification.entity.EmailCampaignRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailCampaignRecipientRepository extends JpaRepository<EmailCampaignRecipient, Long> {

    List<EmailCampaignRecipient> findByCampaignId(Long campaignId);

    Page<EmailCampaignRecipient> findByCampaignIdAndStatus(Long campaignId, String status, Pageable pageable);

    long countByCampaignIdAndStatus(Long campaignId, String status);

    java.util.Optional<EmailCampaignRecipient> findFirstByCampaignIdAndErrorMessageIsNotNull(Long campaignId);
}
