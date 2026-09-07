package com.registration.management.participant.repository;

import com.registration.management.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long>, JpaSpecificationExecutor<Participant> {

    long countByRegistrationId(Long registrationId);

    boolean existsByRegistrationId(Long registrationId);

    java.util.List<Participant> findByEmail(String email);
}
