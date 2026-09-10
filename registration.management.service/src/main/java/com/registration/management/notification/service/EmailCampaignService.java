package com.registration.management.notification.service;

import com.registration.management.auth.entities.User;
import com.registration.management.notification.dto.CampaignRequestDTO;
import com.registration.management.notification.dto.CampaignResponseDTO;
import org.springframework.data.domain.Page;

public interface EmailCampaignService {

    CampaignResponseDTO createCampaign(CampaignRequestDTO request, User currentUser);

    Page<CampaignResponseDTO> getCampaigns(int page, int size);

    CampaignResponseDTO getCampaignById(Long id);

    void cancelCampaign(Long id, User currentUser);

    void sendTestEmail(Long campaignId, User currentUser);

    void sendDirectTestEmail(String email, User currentUser);
}

