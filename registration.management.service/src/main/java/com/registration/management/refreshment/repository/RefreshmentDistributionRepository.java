package com.registration.management.refreshment.repository;

import com.registration.management.refreshment.entity.RefreshmentDistribution;
import com.registration.management.refreshment.enums.DistributionStatus;
import com.registration.management.refreshment.enums.RecipientCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshmentDistributionRepository extends JpaRepository<RefreshmentDistribution, Long>, JpaSpecificationExecutor<RefreshmentDistribution> {

    List<RefreshmentDistribution> findBySessionId(Long sessionId);

    List<RefreshmentDistribution> findBySessionIdAndStatus(Long sessionId, DistributionStatus status);

    Optional<RefreshmentDistribution> findBySessionIdAndParticipantIdAndStatus(Long sessionId, Long participantId, DistributionStatus status);

    Optional<RefreshmentDistribution> findBySessionIdAndSchoolStaffIdAndStatus(Long sessionId, Long schoolStaffId, DistributionStatus status);

    Optional<RefreshmentDistribution> findBySessionIdAndOcMemberIdAndStatus(Long sessionId, Long ocMemberId, DistributionStatus status);

    Optional<RefreshmentDistribution> findBySessionIdAndGuestRecordIdAndStatus(Long sessionId, Long guestId, DistributionStatus status);

    List<RefreshmentDistribution> findByParticipantIdAndStatus(Long participantId, DistributionStatus status);

    List<RefreshmentDistribution> findBySchoolStaffIdAndStatus(Long schoolStaffId, DistributionStatus status);

    List<RefreshmentDistribution> findByOcMemberIdAndStatus(Long ocMemberId, DistributionStatus status);

    List<RefreshmentDistribution> findByGuestRecordIdAndStatus(Long guestId, DistributionStatus status);

    long countBySessionIdAndStatus(Long sessionId, DistributionStatus status);

    @Query("SELECT COUNT(d) FROM RefreshmentDistribution d " +
           "JOIN d.session s " +
           "WHERE s.plan.id = :planId AND d.status = 'GIVEN'")
    long countGivenByPlanId(@Param("planId") Long planId);

    @Query("SELECT COUNT(d) FROM RefreshmentDistribution d " +
           "JOIN d.session s " +
           "WHERE s.plan.id = :planId AND d.recipientCategory = :category AND d.status = 'GIVEN'")
    long countGivenByPlanIdAndCategory(@Param("planId") Long planId, @Param("category") RecipientCategory category);

    @Query("SELECT COUNT(d) FROM RefreshmentDistribution d " +
           "JOIN d.session s " +
           "WHERE s.plan.id = :planId AND s.event.id = :eventId AND d.status = 'GIVEN'")
    long countGivenByPlanIdAndEventId(@Param("planId") Long planId, @Param("eventId") Long eventId);
}
