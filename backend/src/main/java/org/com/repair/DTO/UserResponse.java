package org.com.repair.DTO;

import java.util.List;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Vehicle;

public record UserResponse(
    Long id,
    String username,
    String name,
    String phone,
    String email,
    String address,
    List<Vehicle> vehicles,
    List<RepairOrder> repairOrders
) {
}
