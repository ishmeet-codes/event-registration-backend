package com.registration.management.checkin.specification;

import com.registration.management.checkin.entity.Checkin;
import com.registration.management.checkin.enums.CheckinStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CheckinSpecification {

    public static Specification<Checkin> buildSpecification(
            String search,
            Long eventId,
            Long schoolId,
            Long registrationId,
            CheckinStatus status,
            Long checkedInBy,
            LocalDate date,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";

                Join<Object, Object> participantJoin = root.join("participant", JoinType.LEFT);
                Join<Object, Object> schoolJoin = root.join("school", JoinType.LEFT);
                Join<Object, Object> eventJoin = root.join("event", JoinType.LEFT);

                Predicate participantNameMatch = cb.like(cb.lower(participantJoin.get("fullName")), pattern);
                Predicate schoolNameMatch = cb.like(cb.lower(schoolJoin.get("schoolName")), pattern);
                Predicate schoolCodeMatch = cb.like(cb.lower(schoolJoin.get("schoolCode")), pattern);
                Predicate eventNameMatch = cb.like(cb.lower(eventJoin.get("eventName")), pattern);

                Predicate searchPredicate = cb.or(participantNameMatch, schoolNameMatch, schoolCodeMatch, eventNameMatch);

                try {
                    Long searchId = Long.parseLong(search.trim());
                    Predicate participantIdMatch = cb.equal(participantJoin.get("id"), searchId);
                    Predicate registrationIdMatch = cb.equal(root.get("registration").get("id"), searchId);
                    searchPredicate = cb.or(searchPredicate, participantIdMatch, registrationIdMatch);
                } catch (NumberFormatException ignored) {
                }

                predicates.add(searchPredicate);
            }

            if (eventId != null) {
                predicates.add(cb.equal(root.get("event").get("id"), eventId));
            }

            if (schoolId != null) {
                predicates.add(cb.equal(root.get("school").get("id"), schoolId));
            }

            if (registrationId != null) {
                predicates.add(cb.equal(root.get("registration").get("id"), registrationId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (checkedInBy != null) {
                predicates.add(cb.equal(root.get("checkedInBy").get("id"), checkedInBy));
            }

            if (date != null) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
                predicates.add(cb.between(root.get("checkedInAt"), startOfDay, endOfDay));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkedInAt"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkedInAt"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
