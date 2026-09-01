package com.registration.management.event.service;

import com.registration.management.auth.entities.User;
import com.registration.management.event.dto.EventRequest;
import com.registration.management.event.dto.EventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

public interface EventService {
    Page<EventResponse> list(String search, Boolean active, String participationCategory, LocalDate eventDate, LocalDate eventDateFrom, LocalDate eventDateTo, LocalDate registrationDeadline, Pageable pageable);
    Page<EventResponse> active(String search, Pageable pageable);
    Page<EventResponse> upcoming(String search, Pageable pageable);
    EventResponse get(Long id);
    EventResponse create(EventRequest request, User actor);
    EventResponse update(Long id, EventRequest request, User actor);
    EventResponse updateStatus(Long id, boolean active, User actor);
    void delete(Long id);
}
