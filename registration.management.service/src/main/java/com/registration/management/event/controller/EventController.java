package com.registration.management.event.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.event.dto.EventRequest;
import com.registration.management.event.dto.EventResponse;
import com.registration.management.event.dto.StatusRequest;
import com.registration.management.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequiredArgsConstructor @RequestMapping("/api/events")
public class EventController {
    private final EventService service;
    @GetMapping @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public Page<EventResponse> list(@RequestParam(required = false) String search, @RequestParam(required = false) Boolean active, @RequestParam(required = false) String participationCategory, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDateFrom, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDateTo, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDeadline, @PageableDefault(size = 20, sort = "eventDate") Pageable pageable) { return service.list(search, active, participationCategory, eventDate, eventDateFrom, eventDateTo, registrationDeadline, pageable); }
    @GetMapping("/active") @PreAuthorize("hasAuthority('EVENT_VIEW')") public Page<EventResponse> active(@RequestParam(required = false) String search, @PageableDefault(size = 20, sort = "eventDate") Pageable pageable) { return service.active(search, pageable); }
    @GetMapping("/upcoming") @PreAuthorize("hasAuthority('EVENT_VIEW')") public Page<EventResponse> upcoming(@RequestParam(required = false) String search, @PageableDefault(size = 20, sort = "eventDate") Pageable pageable) { return service.upcoming(search, pageable); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('EVENT_VIEW')") public EventResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @PreAuthorize("hasAuthority('EVENT_CREATE')") public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request, @AuthenticationPrincipal User actor) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, actor)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('EVENT_UPDATE')") public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request, @AuthenticationPrincipal User actor) { return service.update(id, request, actor); }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAuthority('EVENT_STATUS_UPDATE')") public EventResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest request, @AuthenticationPrincipal User actor) { return service.updateStatus(id, request.getActive(), actor); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('EVENT_DELETE')") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
