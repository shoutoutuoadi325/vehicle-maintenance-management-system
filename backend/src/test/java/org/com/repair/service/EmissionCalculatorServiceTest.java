package org.com.repair.service;

import org.com.repair.entity.RepairOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EmissionCalculatorServiceTest {
    @Test
    public void testEmissionCalculation() {
        EmissionCalculatorService service = new EmissionCalculatorService();
        RepairOrder order = new RepairOrder();
        order.setEstimatedHours(2.0);
        order.setEcoMaterial(true);
        order.setReworkCount(0);
        order.setRepairType("repair");
        double emission = service.calculate(order);
        Assertions.assertEquals(2.0, emission, 0.01);
    }

    @Test
    public void testEmissionCalculationWithReworkAndReplace() {
        EmissionCalculatorService service = new EmissionCalculatorService();
        RepairOrder order = new RepairOrder();
        order.setEstimatedHours(2.0);
        order.setEcoMaterial(false);
        order.setReworkCount(1);
        order.setRepairType("replace");
        double emission = service.calculate(order);
        Assertions.assertEquals(9.5, emission, 0.01);
    }
}
