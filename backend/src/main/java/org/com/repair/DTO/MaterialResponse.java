package org.com.repair.DTO;

import org.com.repair.entity.Material;

public record MaterialResponse(
    Long id,
    String name,
    Double unitPrice,
    Integer stockQuantity,
    Integer minimumStockLevel
) {
    public MaterialResponse(Material material) {
        this(
                material.getId(),
                material.getName(),
                material.getUnitPrice(),
                material.getStockQuantity(),
                material.getMinimumStockLevel());
    }
} 