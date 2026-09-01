package com.registration.management.event.service;

import com.registration.management.event.dto.ParticipationCategoryRequest;
import com.registration.management.event.dto.ParticipationCategoryResponse;
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

@Service @RequiredArgsConstructor @Transactional
public class ParticipationCategoryServiceImpl implements ParticipationCategoryService {
    private final ParticipationCategoryRepository repository;
    private final EventRepository eventRepository;

    @Override @Transactional(readOnly = true)
    public Page<ParticipationCategoryResponse> list(String search, Boolean active, Pageable pageable) {
        Specification<ParticipationCategory> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("code")), pattern), cb.like(cb.lower(root.get("displayName")), pattern)));
        }
        if (active != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        return repository.findAll(spec, pageable).map(ParticipationCategoryServiceImpl::toResponse);
    }
    @Override @Transactional(readOnly = true) public ParticipationCategoryResponse get(String code) { return toResponse(find(code)); }
    @Override public ParticipationCategoryResponse create(ParticipationCategoryRequest request) {
        String code = normalizedCode(request.getCode());
        if (repository.existsById(code)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Participation category already exists: " + code);
        validateRange(request);
        return toResponse(repository.save(ParticipationCategory.builder().code(code).displayName(request.getDisplayName().trim()).minParticipants(request.getMinParticipants()).maxParticipants(request.getMaxParticipants()).active(true).build()));
    }
    @Override public ParticipationCategoryResponse update(String code, ParticipationCategoryRequest request) {
        ParticipationCategory category = find(code); validateRange(request);
        String requestCode = normalizedCode(request.getCode());
        if (!category.getCode().equals(requestCode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Participation category code cannot be changed");
        category.setDisplayName(request.getDisplayName().trim()); category.setMinParticipants(request.getMinParticipants()); category.setMaxParticipants(request.getMaxParticipants());
        return toResponse(category);
    }
    @Override public ParticipationCategoryResponse updateStatus(String code, boolean active) { ParticipationCategory category = find(code); category.setActive(active); return toResponse(category); }
    @Override public void delete(String code) {
        ParticipationCategory category = find(code);
        if (eventRepository.existsByParticipationCategory_Code(category.getCode())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Participation category cannot be deleted because events exist");
        repository.delete(category);
    }
    private ParticipationCategory find(String code) { return repository.findById(normalizedCode(code)).orElseThrow(() -> new EntityNotFoundException("Participation category not found: " + code)); }
    private String normalizedCode(String code) { return code == null ? "" : code.trim().toUpperCase(); }
    private void validateRange(ParticipationCategoryRequest request) { if (request.getMinParticipants() > request.getMaxParticipants()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum participants cannot exceed maximum participants"); }
    static ParticipationCategoryResponse toResponse(ParticipationCategory category) { return ParticipationCategoryResponse.builder().code(category.getCode()).displayName(category.getDisplayName()).minParticipants(category.getMinParticipants()).maxParticipants(category.getMaxParticipants()).active(category.isActive()).build(); }
}
