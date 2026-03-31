package org.com.repair.DTO;

import java.util.List;

import org.com.repair.entity.InventoryAlertNotification;

public record InventoryAlertPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        long criticalCount,
        long warningCount,
        List<InventoryAlertNotification> items
) {
}
