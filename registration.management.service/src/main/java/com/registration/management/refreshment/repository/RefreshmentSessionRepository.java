package com.registration.management.refreshment.repository;

import com.registration.management.refreshment.entity.RefreshmentSession;
import com.registration.management.refreshment.enums.RecipientCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshmentSessionRepository extends JpaRepository<RefreshmentSession, Long>, JpaSpecificationExecutor<RefreshmentSession> {

    List<RefreshmentSession> findByPlanIdAndActiveTrueOrderByDisplayOrderAsc(Long planId);

    Optional<RefreshmentSession> findByIdAndActiveTrue(Long id);

    List<RefreshmentSession> findByEventIdAndActiveTrue(Long eventId);

    List<RefreshmentSession> findByPlanIdAndRecipientCategoryAndActiveTrue(Long planId, RecipientCategory recipientCategory);

    @Query("SELECT s FROM RefreshmentSession s " +
           "JOIN s.assignments a " +
           "JOIN a.team t " +
           "JOIN t.members m " +
           "WHERE m.user.id = :userId AND s.active = true AND a.active = true AND t.active = true " +
           "ORDER BY s.displayOrder ASC")
    List<RefreshmentSession> findAssignedSessionsByUserId(@Param("userId") Long userId);
}
