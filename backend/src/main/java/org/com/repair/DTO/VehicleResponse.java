package org.com.repair.DTO;

import java.util.List;
import java.util.stream.Collectors;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.User;
import org.com.repair.entity.Vehicle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleResponse(
    Long id,
    String licensePlate,
    String brand,
    String model,
    Integer year,
    String color,
    String vin,
    UserInfo user,
    List<RepairOrderInfo> repairOrders
) {
    public VehicleResponse(Vehicle vehicle) {
        this(
            vehicle.getId(),
            vehicle.getLicensePlate(),
            vehicle.getBrand(),
            vehicle.getModel(),
            vehicle.getYear(),
            vehicle.getColor(),
            vehicle.getVin(),
            vehicle.getUser() != null ? new UserInfo(vehicle.getUser()) : null,
            vehicle.getRepairOrders() != null ? 
                vehicle.getRepairOrders().stream()
                    .map(RepairOrderInfo::new)
                    .collect(Collectors.toList()) : 
                List.of()
        );
    }

    public VehicleResponse(Vehicle vehicle, List<RepairOrder> repairOrders) {
        this(
            vehicle.getId(),
            vehicle.getLicensePlate(),
            vehicle.getBrand(),
            vehicle.getModel(),
            vehicle.getYear(),
            vehicle.getColor(),
            vehicle.getVin(),
            vehicle.getUser() != null ? new UserInfo(vehicle.getUser()) : null,
            repairOrders != null ? 
                repairOrders.stream()
                    .map(RepairOrderInfo::new)
                    .collect(Collectors.toList()) : 
                List.of()
        );
    }

    public static record UserInfo(
        Long id,
        String username,
        String name,
        String phone,
        String email
    ) {
        public UserInfo(User user) {
            this(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getPhone(),
                user.getEmail()
            );
        }
    }

    public static record RepairOrderInfo(
        Long id,
        String orderNumber,
        String status,
        Double totalCost,
        String description,
        java.util.Date createdAt
    ) {
        public RepairOrderInfo(RepairOrder order) {
            this(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus() != null ? order.getStatus().toString() : "UNKNOWN",
                order.getTotalCost(),
                order.getDescription(),
                order.getCreatedAt()
            );
        }
    }
} 