package com.registration.management.refreshment.repository;

import com.registration.management.refreshment.entity.RefreshmentTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshmentTeamRepository extends JpaRepository<RefreshmentTeam, Long>, JpaSpecificationExecutor<RefreshmentTeam> {
    List<RefreshmentTeam> findByActiveTrueOrderByNameAsc();
    Optional<RefreshmentTeam> findByIdAndActiveTrue(Long id);
}
