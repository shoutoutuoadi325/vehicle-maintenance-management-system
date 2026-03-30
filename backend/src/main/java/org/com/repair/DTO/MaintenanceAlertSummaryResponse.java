package org.com.repair.DTO;

public record MaintenanceAlertSummaryResponse(
        long unreadCount,
        long dueMileageCount,
        long dueTimeCount,
        long upcomingMileageCount,
        long upcomingTimeCount,
        int vehicleHealthScore,
        String riskLevel
) {
}
