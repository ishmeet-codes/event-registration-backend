package com.registration.management.participant.specification;

import com.registration.management.enums.Gender;
import com.registration.management.participant.entity.Participant;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParticipantSpecification {

    public static Specification<Participant> buildSpecification(
            String search,
            Long registrationId,
            Long schoolId,
            Long eventId,
            Gender gender,
            String className,
            LocalDate dobFrom,
            LocalDate dobTo,
            LocalDateTime createdFrom,
            LocalDateTime createdTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate fullNameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate guardianPhoneMatch = cb.like(cb.lower(root.get("guardianPhone")), pattern);
                Predicate classNameMatch = cb.like(cb.lower(root.get("className")), pattern);

                Predicate searchPredicate = cb.or(fullNameMatch, guardianPhoneMatch, classNameMatch);
                try {
                    Long searchId = Long.parseLong(search.trim());
                    Predicate idMatch = cb.equal(root.get("id"), searchId);
                    searchPredicate = cb.or(searchPredicate, idMatch);
                } catch (NumberFormatException ignored) {
                }
                predicates.add(searchPredicate);
            }

            if (registrationId != null) {
                predicates.add(cb.equal(root.get("registration").get("id"), registrationId));
            }

            if (schoolId != null) {
                predicates.add(cb.equal(root.get("registration").get("school").get("id"), schoolId));
            }

            if (eventId != null) {
                predicates.add(cb.equal(root.get("registration").get("event").get("id"), eventId));
            }

            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            if (className != null && !className.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("className")), className.trim().toLowerCase()));
            }

            if (dobFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dob"), dobFrom));
            }

            if (dobTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dob"), dobTo));
            }

            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }

            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
