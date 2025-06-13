package org.com.repair.DTO;

import java.util.Date;
import java.util.Set;

import org.com.repair.entity.RepairOrder.RepairStatus;
import org.com.repair.entity.Technician.SkillType;

public record NewRepairOrderRequest(
    String orderNumber,
    RepairStatus status,
    String description,
    Date createdAt,
    Date updatedAt,
    Date completedAt,
    Double laborCost,
    Double materialCost,
    Double totalCost,
    Double estimatedHours,
    Double actualHours,
    Long userId,
    Long vehicleId,
    Set<Long> technicianIds,
    SkillType requiredSkillType
) {
}