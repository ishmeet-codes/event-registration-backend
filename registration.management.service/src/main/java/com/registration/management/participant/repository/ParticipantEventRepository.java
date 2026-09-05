package com.registration.management.participant.repository;

import com.registration.management.participant.entity.ParticipantEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantEventRepository extends JpaRepository<ParticipantEvent, Long> {
    List<ParticipantEvent> findByParticipantId(Long participantId);
    List<ParticipantEvent> findByParticipantRegistrationId(Long registrationId);
    boolean existsByParticipantIdAndEventId(Long participantId, Long eventId);
    void deleteByParticipantId(Long participantId);
}
