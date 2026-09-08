package com.registration.management.auditManagement.specification;

import com.registration.management.audit.entities.AuditLog;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuditLogSpecification {

    public static final List<AuditAction> SECURITY_ACTIONS = Arrays.asList(
            AuditAction.LOGIN_SUCCESS,
            AuditAction.LOGIN_FAILED,
            AuditAction.LOGOUT,
            AuditAction.PASSWORD_CHANGED,
            AuditAction.PASSWORD_RESET,
            AuditAction.ACCESS_DENIED,
            AuditAction.UNAUTHORIZED_ACCESS_ATTEMPT,
            AuditAction.TOKEN_REFRESH,
            AuditAction.LOGIN
    );

    public static Specification<AuditLog> withFilters(
            String search,
            Long userId,
            String module,
            AuditAction action,
            String entityType,
            Long entityId,
            AuditStatus status,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Boolean securityOnly
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search (multi-field)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                List<Predicate> searchPredicates = new ArrayList<>();

                // User name & email (join user if exists)
                searchPredicates.add(cb.like(cb.lower(root.get("user").get("fullName")), searchPattern));
                searchPredicates.add(cb.like(cb.lower(root.get("user").get("email")), searchPattern));

                // Module
                searchPredicates.add(cb.like(cb.lower(root.get("module")), searchPattern));

                // Entity name
                searchPredicates.add(cb.like(cb.lower(root.get("entityName")), searchPattern));

                // Description
                searchPredicates.add(cb.like(cb.lower(root.get("description")), searchPattern));

                // Error message
                searchPredicates.add(cb.like(cb.lower(root.get("errorMessage")), searchPattern));

                // Request ID
                searchPredicates.add(cb.like(cb.lower(root.get("requestId")), searchPattern));

                // Action name
                searchPredicates.add(cb.like(cb.lower(root.get("action").as(String.class)), searchPattern));

                // Numeric match for entityId
                try {
                    Long numericSearch = Long.parseLong(search.trim());
                    searchPredicates.add(cb.equal(root.get("entityId"), numericSearch));
                } catch (NumberFormatException ignored) {
                }

                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            // 2. User ID
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            // 3. Module
            if (module != null && !module.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("module")), module.trim().toUpperCase()));
            }

            // 4. Action
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            // 5. Entity Type
            if (entityType != null && !entityType.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("entityName")), entityType.trim().toUpperCase()));
            }

            // 6. Entity ID
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }

            // 7. Status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 8. Date Range
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            // 9. Security Only
            if (Boolean.TRUE.equals(securityOnly)) {
                Predicate isSecurityAction = root.get("action").in(SECURITY_ACTIONS);
                Predicate isSecurityModule = cb.or(
                        cb.equal(cb.upper(root.get("module")), "AUTH"),
                        cb.equal(cb.upper(root.get("module")), "SECURITY")
                );
                predicates.add(cb.or(isSecurityAction, isSecurityModule));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
