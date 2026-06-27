package org.com.repair.service;

import org.com.repair.entity.RepairOrder;
import org.com.repair.service.green.GreenEmissionEngine;
import org.com.repair.service.green.GreenIndexMappingResult;
import org.com.repair.service.green.GreenOrderCarbonSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmissionCalculatorService {
    
    private final GreenEmissionEngine greenEmissionEngine;

    @Autowired
    public EmissionCalculatorService(GreenEmissionEngine greenEmissionEngine) {
        this.greenEmissionEngine = greenEmissionEngine;
    }

    public GreenIndexMappingResult calculateAndMap(RepairOrder order) {
        GreenOrderCarbonSnapshot snapshot = new GreenOrderCarbonSnapshot(
            order.getEstimatedHours(),
            order.isEcoMaterial(),
            order.getReworkCount(),
            order.getRepairType(),
            null
        );
        double emission = greenEmissionEngine.calculateRelativeEmission(snapshot, null);
        return greenEmissionEngine.mapToGreenIndex(emission);
    }

    public double calculate(RepairOrder order) {
        return calculateAndMap(order).estimatedEmission();
    }
}
