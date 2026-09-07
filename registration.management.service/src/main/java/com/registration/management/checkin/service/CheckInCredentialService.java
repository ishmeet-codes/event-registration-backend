package com.registration.management.checkin.service;

import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.CredentialResponseDTO;
import com.registration.management.checkin.entity.CheckInCredential;
import com.registration.management.registration.entity.Registration;

import java.util.List;

public interface CheckInCredentialService {

    List<CheckInCredential> createCredentialsForApprovedRegistration(Registration registration);

    List<CredentialResponseDTO> getMyActiveCredentials(User currentUser);

    List<CredentialResponseDTO> getCredentialsByRegistrationId(Long registrationId);

    CredentialResponseDTO regenerateCredential(Long credentialId, User currentUser);

    void revokeCredentialsForRegistration(Long registrationId);

    String hashToken(String rawToken);

    String generateOpaqueToken();

    CredentialResponseDTO toDto(CheckInCredential credential, String rawToken);
}
