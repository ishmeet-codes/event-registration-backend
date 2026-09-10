package com.registration.management.notification.repository;

import com.registration.management.notification.entity.EmailAutomation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailAutomationRepository extends JpaRepository<EmailAutomation, Long> {

    List<EmailAutomation> findByTriggerTypeAndActiveTrue(String triggerType);

    List<EmailAutomation> findByActiveTrue();
}
