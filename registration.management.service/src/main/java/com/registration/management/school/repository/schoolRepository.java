package com.registration.management.school.repository;

import com.registration.management.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface schoolRepository extends JpaRepository<School, Long> {

    boolean existsBySchoolCode(String schoolCode);

    Optional<School> findBySchoolCode(String schoolCode);
}
