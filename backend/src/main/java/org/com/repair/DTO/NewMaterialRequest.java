package org.com.repair.DTO;

public record NewMaterialRequest(
    String name,
    Double unitPrice,
    Integer stockQuantity,
    Integer minimumStockLevel
) {
} 