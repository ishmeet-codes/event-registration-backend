package com.registration.management.school.repository;

import com.registration.management.enums.StaffRole;
import com.registration.management.school.entity.SchoolStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface schoolStaffRepository extends JpaRepository<SchoolStaff, Long>, JpaSpecificationExecutor<SchoolStaff> {

    Optional<SchoolStaff> findByIdAndSchoolId(Long id, Long schoolId);

    long countBySchoolIdAndStaffRoleAndActiveTrue(Long schoolId, StaffRole staffRole);

    long countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(Long schoolId, StaffRole staffRole, Long staffId);

    boolean existsBySchoolIdAndStaffRoleAndActiveTrue(Long schoolId, StaffRole staffRole);

    boolean existsByIdAndSchoolId(Long id, Long schoolId);

    boolean existsByUserIdAndActiveTrue(Long userId);

    boolean existsByUserIdAndStaffRoleAndActiveTrue(Long userId, StaffRole staffRole);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s.school.id FROM SchoolStaff s WHERE s.user.id = :userId AND s.active = true")
    java.util.List<Long> findSchoolIdsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
