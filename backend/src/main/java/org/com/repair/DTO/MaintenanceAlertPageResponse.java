package org.com.repair.DTO;

import java.util.List;

public record MaintenanceAlertPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<MaintenanceAlertResponse> items
) {
}
