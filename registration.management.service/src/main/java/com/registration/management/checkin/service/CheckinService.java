package com.registration.management.checkin.service;

import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.*;
import com.registration.management.checkin.enums.CheckinStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CheckinService {

    CheckinResponseDTO checkinParticipant(CheckinRequestDTO request, User currentActor);

    CheckinResponseDTO getCheckinById(Long id);

    CheckinResponseDTO correctCheckin(Long id, CheckinRequestDTO request, User currentActor);

    void deleteCheckin(Long id, User currentActor);

    Page<CheckinResponseDTO> getCheckins(
            String search,
            Long eventId,
            Long schoolId,
            Long registrationId,
            CheckinStatus status,
            Long checkedInBy,
            LocalDate date,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    );

    CheckinResponseDTO updateStatus(Long id, CheckinStatusUpdateRequestDTO request, User currentActor);

    CheckinResponseDTO checkoutParticipant(Long id, CheckoutRequestDTO request, User currentActor);

    EventParticipantAttendanceDTO getParticipantAttendance(Long participantId);

    List<CheckinResponseDTO> getEventCheckins(Long eventId);

    List<CheckinResponseDTO> getRegistrationCheckins(Long registrationId);

    List<CheckinResponseDTO> getSchoolCheckins(Long schoolId);

    EventCheckinSummaryDTO getEventSummary(Long eventId);

    List<EventParticipantAttendanceDTO> getEventParticipantsAttendance(Long eventId);

    BulkCheckinResponseDTO bulkCheckin(BulkCheckinRequestDTO request, User currentActor);

    BulkCheckinResponseDTO bulkCheckinByEvent(Long eventId, BulkCheckinRequestDTO request, User currentActor);

    QrScanResponseDTO processQrScan(QrScanRequestDTO request, User scannerUser);
}
