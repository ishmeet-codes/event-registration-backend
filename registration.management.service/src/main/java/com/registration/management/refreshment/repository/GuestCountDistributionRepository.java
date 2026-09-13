package com.registration.management.refreshment.repository;

import com.registration.management.refreshment.entity.GuestCountDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestCountDistributionRepository extends JpaRepository<GuestCountDistribution, Long> {
    List<GuestCountDistribution> findBySessionIdOrderByDistributedAtDesc(Long sessionId);

    @Query("SELECT COALESCE(SUM(g.countDistributed), 0) FROM GuestCountDistribution g WHERE g.session.id = :sessionId")
    int sumCountDistributedBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT COALESCE(SUM(g.countDistributed), 0) FROM GuestCountDistribution g WHERE g.session.plan.id = :planId")
    int sumCountDistributedByPlanId(@Param("planId") Long planId);
}
