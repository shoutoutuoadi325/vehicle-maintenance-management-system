package org.com.repair.service;

import org.com.repair.entity.RepairOrder;
import org.springframework.stereotype.Service;

@Service
public class EmissionCalculatorService {
    // 系数可根据实际需求调整
    private static final double ECO_MATERIAL_FACTOR = 1.0;
    private static final double NORMAL_MATERIAL_FACTOR = 2.0;
    private static final double REWORK_FACTOR = 1.5;
    private static final double REPAIR_TYPE_FACTOR = 1.0;
    private static final double REPLACE_TYPE_FACTOR = 2.0;

    public double calculate(RepairOrder order) {
        double workHours = order.getEstimatedHours() != null ? order.getEstimatedHours() : 1.0;
        double materialFactor = order.isEcoMaterial() ? ECO_MATERIAL_FACTOR : NORMAL_MATERIAL_FACTOR;
        double typeFactor = "repair".equalsIgnoreCase(order.getRepairType()) ? REPAIR_TYPE_FACTOR : REPLACE_TYPE_FACTOR;
        double emission = workHours * materialFactor * typeFactor + order.getReworkCount() * REWORK_FACTOR;
        return emission;
    }
}
