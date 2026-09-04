package com.registration.management.registration.repository;

import com.registration.management.enums.RegistrationStatus;
import com.registration.management.registration.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long>, JpaSpecificationExecutor<Registration> {

    boolean existsBySchoolIdAndEventId(Long schoolId, Long eventId);

    long countByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    long countBySchoolId(Long schoolId);

    long countBySchoolIdAndStatus(Long schoolId, RegistrationStatus status);

    long countBySchoolIdAndEventId(Long schoolId, Long eventId);

    long countBySchoolIdAndEventIdAndStatus(Long schoolId, Long eventId, RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    @Query("SELECT COUNT(p) FROM Participant p WHERE p.registration.id = :registrationId")
    long countParticipantsByRegistrationId(@Param("registrationId") Long registrationId);

    @Query("SELECT r FROM Registration r " +
            "LEFT JOIN FETCH r.school " +
            "LEFT JOIN FETCH r.event " +
            "LEFT JOIN FETCH r.createdByStaff " +
            "WHERE r.id = :id")
    Optional<Registration> findByIdWithDetails(@Param("id") Long id);
}
