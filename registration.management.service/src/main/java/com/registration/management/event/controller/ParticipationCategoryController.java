package com.registration.management.event.controller;

import com.registration.management.event.dto.ParticipationCategoryRequest;
import com.registration.management.event.dto.ParticipationCategoryResponse;
import com.registration.management.event.dto.StatusRequest;
import com.registration.management.event.service.ParticipationCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/participation-categories")
public class ParticipationCategoryController {
    private final ParticipationCategoryService service;
    @GetMapping @PreAuthorize("hasAuthority('PARTICIPATION_CATEGORY_VIEW')")
    public Page<ParticipationCategoryResponse> list(@RequestParam(required = false) String search, @RequestParam(required = false) Boolean active, @PageableDefault(size = 20, sort = "displayName") Pageable pageable) { return service.list(search, active, pageable); }
    @GetMapping("/{code}") @PreAuthorize("hasAuthority('PARTICIPATION_CATEGORY_VIEW')") public ParticipationCategoryResponse get(@PathVariable String code) { return service.get(code); }
    @PostMapping @PreAuthorize("hasAuthority('PARTICIPATION_CATEGORY_CREATE')") public ResponseEntity<ParticipationCategoryResponse> create(@Valid @RequestBody ParticipationCategoryRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @PutMapping("/{code}") @PreAuthorize("hasAuthority('PARTICIPATION_CATEGORY_UPDATE')") public ParticipationCategoryResponse update(@PathVariable String code, @Valid @RequestBody ParticipationCategoryRequest request) { return service.update(code, request); }
    @PatchMapping("/{code}/status") @PreAuthorize("hasAuthority('PARTICIPATION_CATEGORY_STATUS_UPDATE')") public ParticipationCategoryResponse status(@PathVariable String code, @Valid @RequestBody StatusRequest request) { return service.updateStatus(code, request.getActive()); }
    @DeleteMapping("/{code}") @PreAuthorize("hasAuthority('PARTICIPATION_CATEGORY_DELETE')") public ResponseEntity<Void> delete(@PathVariable String code) { service.delete(code); return ResponseEntity.noContent().build(); }
}
