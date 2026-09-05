package com.registration.management.registration.repository;

import com.registration.management.registration.entity.RegistrationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationEventRepository extends JpaRepository<RegistrationEvent, Long> {
    List<RegistrationEvent> findByRegistrationId(Long registrationId);
    boolean existsByRegistrationIdAndEventId(Long registrationId, Long eventId);
    long countByEventId(Long eventId);
    void deleteByRegistrationId(Long registrationId);
}
