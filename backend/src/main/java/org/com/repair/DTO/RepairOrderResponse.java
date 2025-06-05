package org.com.repair.DTO;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.RepairOrder.RepairStatus;
import org.com.repair.entity.Technician.SkillType;

public record RepairOrderResponse(
    Long id,
    String orderNumber,
    RepairStatus status,
    String description,
    Date createdAt,
    Date updatedAt,
    Date startedAt,
    Date completedAt,
    Double laborCost,
    Double materialCost,
    Double totalCost,
    Double estimatedHours,
    Double actualHours,
    String assignmentType,
    SkillType requiredSkillType,
    UserInfo user,
    VehicleInfo vehicle,
    List<TechnicianInfo> technicians
) {
    public RepairOrderResponse(RepairOrder repairOrder) {
        this(
            repairOrder.getId(),
            repairOrder.getOrderNumber(),
            repairOrder.getStatus(),
            repairOrder.getDescription(),
            repairOrder.getCreatedAt(),
            repairOrder.getUpdatedAt(),
            repairOrder.getStartedAt(),
            repairOrder.getCompletedAt(),
            repairOrder.getLaborCost(),
            repairOrder.getMaterialCost(),
            repairOrder.getTotalCost(),
            repairOrder.getEstimatedHours(),
            repairOrder.getActualHours(),
            repairOrder.getAssignmentType() != null ? repairOrder.getAssignmentType().toString() : "AUTO",
            repairOrder.getRequiredSkillType(),
            repairOrder.getUser() != null ? new UserInfo(
                repairOrder.getUser().getId(),
                repairOrder.getUser().getName(),
                repairOrder.getUser().getUsername(),
                repairOrder.getUser().getPhone(),
                repairOrder.getUser().getEmail()
            ) : null,
            repairOrder.getVehicle() != null ? new VehicleInfo(
                repairOrder.getVehicle().getId(),
                repairOrder.getVehicle().getLicensePlate(),
                repairOrder.getVehicle().getBrand(),
                repairOrder.getVehicle().getModel(),
                repairOrder.getVehicle().getYear()
            ) : null,
            repairOrder.getTechnicians() != null ? 
                repairOrder.getTechnicians().stream()
                    .map(tech -> new TechnicianInfo(
                        tech.getId(),
                        tech.getName(),
                        tech.getEmployeeId(),
                        tech.getSkillType(),
                        tech.getHourlyRate()
                    ))
                    .collect(Collectors.toList()) : 
                List.of()
        );
    }
    
    public static record UserInfo(
        Long id,
        String name,
        String username,
        String phone,
        String email
    ) {}
    
    public static record VehicleInfo(
        Long id,
        String licensePlate,
        String brand,
        String model,
        Integer year
    ) {}
    
    public static record TechnicianInfo(
        Long id,
        String name,
        String employeeId,
        org.com.repair.entity.Technician.SkillType skillType,
        Double hourlyRate
    ) {}
}