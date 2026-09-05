package com.registration.management.participant.service;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.Gender;
import com.registration.management.participant.dto.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface ParticipantService {

    ParticipantResponseDTO createParticipant(ParticipantCreateRequestDTO request, User currentUser);

    ParticipantResponseDTO getParticipantById(Long id, User currentUser);

    Page<ParticipantResponseDTO> getParticipants(
            String search,
            Long registrationId,
            Long schoolId,
            Long eventId,
            Gender gender,
            String className,
            LocalDate dobFrom,
            LocalDate dobTo,
            int page,
            int size,
            String sort,
            User currentUser
    );

    ParticipantResponseDTO updateParticipant(Long id, ParticipantUpdateRequestDTO request, User currentUser);

    void deleteParticipant(Long id, User currentUser);

    Page<ParticipantResponseDTO> getParticipantsByRegistration(
            Long registrationId,
            String search,
            Gender gender,
            String className,
            int page,
            int size,
            String sort,
            User currentUser
    );

    Page<ParticipantResponseDTO> getParticipantsBySchool(
            Long schoolId,
            String search,
            Long eventId,
            Gender gender,
            String className,
            int page,
            int size,
            String sort,
            User currentUser
    );

    Page<ParticipantResponseDTO> getParticipantsByEvent(
            Long eventId,
            String search,
            Long schoolId,
            Gender gender,
            String className,
            int page,
            int size,
            String sort,
            User currentUser
    );

    ParticipantCountResponseDTO getParticipantCount(Long registrationId, User currentUser);

    ParticipantEligibilityResponseDTO checkEligibility(Long id, User currentUser);
}
