package com.registration.management.registration.repository;

import com.registration.management.registration.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long>, JpaSpecificationExecutor<Participant> {

    long countByRegistrationId(Long registrationId);

    boolean existsByRegistrationId(Long registrationId);
}
