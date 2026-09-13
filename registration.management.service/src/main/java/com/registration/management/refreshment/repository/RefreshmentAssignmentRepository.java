package com.registration.management.refreshment.repository;

import com.registration.management.refreshment.entity.RefreshmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshmentAssignmentRepository extends JpaRepository<RefreshmentAssignment, Long> {
    List<RefreshmentAssignment> findBySessionIdAndActiveTrue(Long sessionId);
    List<RefreshmentAssignment> findByTeamIdAndActiveTrue(Long teamId);
    Optional<RefreshmentAssignment> findBySessionIdAndTeamId(Long sessionId, Long teamId);
    void deleteBySessionId(Long sessionId);
}
