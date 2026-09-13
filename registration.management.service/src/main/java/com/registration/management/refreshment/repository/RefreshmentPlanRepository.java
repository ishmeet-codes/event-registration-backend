package com.registration.management.refreshment.repository;

import com.registration.management.refreshment.entity.RefreshmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshmentPlanRepository extends JpaRepository<RefreshmentPlan, Long>, JpaSpecificationExecutor<RefreshmentPlan> {
    List<RefreshmentPlan> findByActiveTrueOrderByEventDateDesc();
    List<RefreshmentPlan> findByEventDateAndActiveTrue(LocalDate eventDate);
    Optional<RefreshmentPlan> findByIdAndActiveTrue(Long id);
}
