package org.com.repair.DTO;

public record NewVehicleRequest(
    String licensePlate,
    String brand,
    String model,
    Integer year,
    String color,
    String vin,
    Long userId,
    Integer mileage,
    java.util.Date lastMaintenanceDate
) {
} 