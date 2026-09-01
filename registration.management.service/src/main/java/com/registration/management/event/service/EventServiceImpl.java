package com.registration.management.event.service;

import com.registration.management.auth.entities.User;
import com.registration.management.event.dto.EventRequest;
import com.registration.management.event.dto.EventResponse;
import com.registration.management.event.dto.ParticipationCategoryResponse;
import com.registration.management.event.entities.Event;
import com.registration.management.event.entities.ParticipationCategory;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.event.repository.ParticipationCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;

@Service @RequiredArgsConstructor @Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final ParticipationCategoryRepository categoryRepository;

    @Override @Transactional(readOnly = true)
    public Page<EventResponse> list(String search, Boolean active, String participationCategory, LocalDate eventDate, LocalDate eventDateFrom, LocalDate eventDateTo, LocalDate registrationDeadline, Pageable pageable) {
        Specification<Event> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) { String p = "%" + search.trim().toLowerCase() + "%"; spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("eventName")), p), cb.like(cb.lower(root.get("description")), p), cb.like(cb.lower(root.get("venue")), p))); }
        if (active != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        if (participationCategory != null && !participationCategory.isBlank()) spec = spec.and((root, query, cb) -> cb.equal(root.join("participationCategory").get("code"), participationCategory.trim().toUpperCase()));
        if (eventDate != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("eventDate"), eventDate));
        if (eventDateFrom != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), eventDateFrom));
        if (eventDateTo != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), eventDateTo));
        if (registrationDeadline != null) spec = spec.and((root, query, cb) -> cb.lessThan(root.get("registrationDeadline"), registrationDeadline.plusDays(1).atStartOfDay()));
        return eventRepository.findAll(spec, pageable).map(this::toResponse);
    }
    @Override @Transactional(readOnly = true) public Page<EventResponse> active(String search, Pageable pageable) { return list(search, true, null, null, null, null, null, pageable); }
    @Override @Transactional(readOnly = true) public Page<EventResponse> upcoming(String search, Pageable pageable) { return list(search, true, null, null, LocalDate.now(), null, null, pageable); }
    @Override @Transactional(readOnly = true) public EventResponse get(Long id) { return toResponse(find(id)); }
    @Override public EventResponse create(EventRequest request, User actor) { Event event = new Event(); apply(event, request); event.setActive(true); event.setCreatedBy(actor); event.setUpdatedBy(actor); return toResponse(eventRepository.save(event)); }
    @Override public EventResponse update(Long id, EventRequest request, User actor) { Event event = find(id); apply(event, request); event.setUpdatedBy(actor); return toResponse(event); }
    @Override public EventResponse updateStatus(Long id, boolean active, User actor) { Event event = find(id); event.setActive(active); event.setUpdatedBy(actor); return toResponse(event); }
    @Override public void delete(Long id) { Event event = find(id); if (eventRepository.countRegistrations(id) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Event cannot be deleted because registrations already exist"); eventRepository.delete(event); }
    private void apply(Event event, EventRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must be before end time");
        if (!request.getRegistrationDeadline().toLocalDate().isBefore(request.getEventDate())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration deadline must be before the event date");
        ParticipationCategory category = categoryRepository.findById(request.getParticipationCategoryCode().trim().toUpperCase()).orElseThrow(() -> new EntityNotFoundException("Participation category not found: " + request.getParticipationCategoryCode()));
        if (!category.isActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Participation category must be active");
        event.setParticipationCategory(category); event.setEventName(request.getEventName().trim()); event.setDescription(request.getDescription().trim()); event.setVenue(request.getVenue().trim()); event.setRegistrationDeadline(request.getRegistrationDeadline()); event.setEventDate(request.getEventDate()); event.setStartTime(request.getStartTime()); event.setEndTime(request.getEndTime()); event.setMaxRegistrations(request.getMaxRegistrations());
    }
    private Event find(Long id) { return eventRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Event not found: " + id)); }
    private EventResponse toResponse(Event event) { long current = eventRepository.countRegistrations(event.getId()); return EventResponse.builder().id(event.getId()).participationCategory(toCategory(event.getParticipationCategory())).eventName(event.getEventName()).description(event.getDescription()).venue(event.getVenue()).registrationDeadline(event.getRegistrationDeadline()).eventDate(event.getEventDate()).startTime(event.getStartTime()).endTime(event.getEndTime()).maxRegistrations(event.getMaxRegistrations()).currentRegistrations(current).remainingSlots(Math.max(0, event.getMaxRegistrations() - current)).active(event.isActive()).createdAt(event.getCreatedAt()).updatedAt(event.getUpdatedAt()).build(); }
    private ParticipationCategoryResponse toCategory(ParticipationCategory category) { return ParticipationCategoryServiceImpl.toResponse(category); }
}
