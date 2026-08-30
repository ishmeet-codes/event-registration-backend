package com.registration.management.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Returned by the bulk-import endpoint with per-row success/failure details.
 */
@Data
@Builder
public class BulkImportResultDto {

    /** Number of rows successfully created. */
    private int successCount;

    /** Number of rows that failed. */
    private int failureCount;

    /** Per-row results (only failures are required, but all rows are included). */
    private List<RowResult> rows;

    @Data
    @Builder
    public static class RowResult {
        private int rowNumber;
        private String email;
        private String fullName;
        private String roleCode;
        /** Generated user ID on success, null on failure. */
        private Long userId;
        private boolean success;
        private String error;
    }
}
