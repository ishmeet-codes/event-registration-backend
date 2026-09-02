package com.registration.management.school.repository;

import com.registration.management.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface schoolRepository extends JpaRepository<School, Long>, JpaSpecificationExecutor<School> {

    boolean existsBySchoolCode(String schoolCode);

    Optional<School> findBySchoolCode(String schoolCode);

    @Query("SELECT COUNT(s) FROM SchoolStaff s WHERE s.school.id = :schoolId")
    long countStaffBySchoolId(@Param("schoolId") Long schoolId);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.school.id = :schoolId")
    long countRegistrationsBySchoolId(@Param("schoolId") Long schoolId);
}
