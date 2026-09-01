package com.registration.management.event.service;

import com.registration.management.event.dto.ParticipationCategoryRequest;
import com.registration.management.event.dto.ParticipationCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ParticipationCategoryService {
    Page<ParticipationCategoryResponse> list(String search, Boolean active, Pageable pageable);
    ParticipationCategoryResponse get(String code);
    ParticipationCategoryResponse create(ParticipationCategoryRequest request);
    ParticipationCategoryResponse update(String code, ParticipationCategoryRequest request);
    ParticipationCategoryResponse updateStatus(String code, boolean active);
    void delete(String code);
}
