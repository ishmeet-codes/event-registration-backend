package com.registration.management.event.repository;

import com.registration.management.event.entities.ParticipationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ParticipationCategoryRepository extends JpaRepository<ParticipationCategory, String>, JpaSpecificationExecutor<ParticipationCategory> {
}
