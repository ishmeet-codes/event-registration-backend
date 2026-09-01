package com.registration.management.event.repository;

import com.registration.management.event.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    boolean existsByParticipationCategory_Code(String code);

    @Query("select count(r) from Registration r where r.event.id = :eventId")
    long countRegistrations(@Param("eventId") Long eventId);
}
