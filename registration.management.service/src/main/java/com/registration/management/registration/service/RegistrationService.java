package com.registration.management.registration.service;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.registration.dto.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface RegistrationService {

    RegistrationResponseDTO getRegistrationById(Long id);

    Page<RegistrationResponseDTO> getRegistrations(
            String search,
            List<RegistrationStatus> statuses,
            Long schoolId,
            Long eventId,
            Long createdByStaffId,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate eventDateFrom,
            LocalDate eventDateTo,
            int page,
            int size,
            String sort
    );

    RegistrationResponseDTO createRegistration(RegistrationCreateRequestDTO request, User currentUser);

    RegistrationResponseDTO updateRegistration(Long id, RegistrationUpdateRequestDTO request, User currentUser);

    void deleteRegistration(Long id, User currentUser);

    RegistrationResponseDTO updateRegistrationStatus(Long id, RegistrationStatusUpdateRequestDTO request, User currentUser);

    RegistrationResponseDTO submitRegistration(Long id, RegistrationRemarksRequestDTO request, User currentUser);

    RegistrationResponseDTO approveRegistration(Long id, User currentUser);

    RegistrationResponseDTO rejectRegistration(Long id, RegistrationRemarksRequestDTO request, User currentUser);

    RegistrationResponseDTO cancelRegistration(Long id, RegistrationRemarksRequestDTO request, User currentUser);

    RegistrationStatisticsDTO getRegistrationStatistics(Long eventId, Long schoolId);

    Page<RegistrationResponseDTO> getRegistrationsForEvent(
            Long eventId,
            String search,
            List<RegistrationStatus> statuses,
            int page,
            int size,
            String sort
    );

    Page<RegistrationResponseDTO> getRegistrationsForSchool(
            Long schoolId,
            String search,
            List<RegistrationStatus> statuses,
            Long eventId,
            int page,
            int size,
            String sort,
            User currentUser
    );
}
