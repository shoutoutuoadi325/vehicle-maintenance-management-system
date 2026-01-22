package org.com.repair.DTO;

public record DiagnosisRequest(
    String description,
    String vehicleBrand,
    String vehicleModel,
    Integer mileage
) {}
