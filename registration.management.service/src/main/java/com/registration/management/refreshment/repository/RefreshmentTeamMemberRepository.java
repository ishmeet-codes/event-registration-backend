package com.registration.management.refreshment.repository;

import com.registration.management.refreshment.entity.RefreshmentTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefreshmentTeamMemberRepository extends JpaRepository<RefreshmentTeamMember, Long> {
    List<RefreshmentTeamMember> findByTeamId(Long teamId);
    List<RefreshmentTeamMember> findByUserId(Long userId);
    void deleteByTeamId(Long teamId);
}
