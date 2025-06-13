package org.com.repair.DTO;

import org.com.repair.entity.User;

public record UserWithStatsResponse(
    Long id,
    String username,
    String name,
    String phone,
    String email,
    String address,
    Integer vehicleCount,
    Integer orderCount
) {
    public UserWithStatsResponse(User user) {
        this(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getPhone(),
            user.getEmail(),
            user.getAddress(),
            user.getVehicles() != null ? user.getVehicles().size() : 0,
            user.getRepairOrders() != null ? user.getRepairOrders().size() : 0
        );
    }
} 