package org.com.repair.DTO;

public record AdminSelfIterationApprovalRequest(
        String reviewer,
        String reviewNote
) {
}
