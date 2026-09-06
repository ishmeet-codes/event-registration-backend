package com.registration.management.checkin.repository;

import com.registration.management.checkin.entity.Checkin;
import com.registration.management.checkin.enums.CheckinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, Long>, JpaSpecificationExecutor<Checkin> {

    boolean existsByParticipantId(Long participantId);

    Optional<Checkin> findByParticipantId(Long participantId);

    List<Checkin> findByEventId(Long eventId);

    List<Checkin> findByRegistrationId(Long registrationId);

    List<Checkin> findBySchoolId(Long schoolId);

    long countByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, CheckinStatus status);
}
