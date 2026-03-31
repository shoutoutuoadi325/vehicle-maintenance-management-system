package org.com.repair.DTO;

import java.util.Date;

public record DispatchBoardOrderResponse(
        Long id,
        String orderNumber,
        String status,
        String description,
        String requiredSkillType,
        String customerName,
        String vehiclePlate,
        String assignmentType,
        boolean aiAssigned,
        Date createdAt,
        Double estimatedHours
) {
}
