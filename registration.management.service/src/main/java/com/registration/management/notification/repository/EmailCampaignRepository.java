package com.registration.management.notification.repository;

import com.registration.management.notification.entity.EmailCampaign;
import com.registration.management.notification.enums.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {

    Page<EmailCampaign> findByStatus(CampaignStatus status, Pageable pageable);

    @Query("SELECT c FROM EmailCampaign c WHERE c.status = 'SCHEDULED' AND c.scheduledAt <= :now")
    List<EmailCampaign> findDueScheduledCampaigns(@Param("now") LocalDateTime now);

    Page<EmailCampaign> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
