package com.registration.management.refreshment.repository;

import com.registration.management.refreshment.entity.GuestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestRecordRepository extends JpaRepository<GuestRecord, Long>, JpaSpecificationExecutor<GuestRecord> {
    List<GuestRecord> findByPlanIdAndActiveTrue(Long planId);
    Optional<GuestRecord> findByIdAndActiveTrue(Long id);
    List<GuestRecord> findByPlanIdAndNameContainingIgnoreCaseAndActiveTrue(Long planId, String name);
}
