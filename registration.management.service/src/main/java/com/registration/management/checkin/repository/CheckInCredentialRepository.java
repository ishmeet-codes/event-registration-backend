package com.registration.management.checkin.repository;

import com.registration.management.checkin.entity.CheckInCredential;
import com.registration.management.checkin.enums.CredentialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInCredentialRepository extends JpaRepository<CheckInCredential, Long> {

    Optional<CheckInCredential> findByTokenHash(String tokenHash);

    Optional<CheckInCredential> findByTokenHashAndActiveTrue(String tokenHash);

    List<CheckInCredential> findByParticipantIdAndActiveTrue(Long participantId);

    List<CheckInCredential> findBySchoolStaffIdAndActiveTrue(Long schoolStaffId);

    List<CheckInCredential> findByRegistrationId(Long registrationId);

    List<CheckInCredential> findByRegistrationIdAndActiveTrue(Long registrationId);

    Optional<CheckInCredential> findFirstByParticipantIdAndEventIdAndActiveTrue(Long participantId, Long eventId);

    Optional<CheckInCredential> findFirstBySchoolStaffIdAndEventIdAndCredentialTypeAndActiveTrue(Long schoolStaffId, Long eventId, CredentialType credentialType);
}
