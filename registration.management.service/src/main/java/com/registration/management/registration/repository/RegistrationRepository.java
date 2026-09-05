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

    @Query("SELECT COUNT(r) > 0 FROM Registration r JOIN r.registrationEvents re WHERE r.school.id = :schoolId AND re.event.id = :eventId")
    boolean existsBySchoolIdAndEventId(@Param("schoolId") Long schoolId, @Param("eventId") Long eventId);

    @Query("SELECT COUNT(DISTINCT r) FROM Registration r JOIN r.registrationEvents re WHERE re.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(DISTINCT r) FROM Registration r JOIN r.registrationEvents re WHERE re.event.id = :eventId AND r.status = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") RegistrationStatus status);

    long countBySchoolId(Long schoolId);

    long countBySchoolIdAndStatus(Long schoolId, RegistrationStatus status);

    @Query("SELECT COUNT(DISTINCT r) FROM Registration r JOIN r.registrationEvents re WHERE r.school.id = :schoolId AND re.event.id = :eventId")
    long countBySchoolIdAndEventId(@Param("schoolId") Long schoolId, @Param("eventId") Long eventId);

    @Query("SELECT COUNT(DISTINCT r) FROM Registration r JOIN r.registrationEvents re WHERE r.school.id = :schoolId AND re.event.id = :eventId AND r.status = :status")
    long countBySchoolIdAndEventIdAndStatus(@Param("schoolId") Long schoolId, @Param("eventId") Long eventId, @Param("status") RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    @Query("SELECT COUNT(p) FROM Participant p WHERE p.registration.id = :registrationId")
    long countParticipantsByRegistrationId(@Param("registrationId") Long registrationId);

    @Query("SELECT r FROM Registration r " +
            "LEFT JOIN FETCH r.school " +
            "LEFT JOIN FETCH r.createdByStaff " +
            "WHERE r.id = :id")
    Optional<Registration> findByIdWithDetails(@Param("id") Long id);
}
