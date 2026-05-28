package org.com.repair.service;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.RepairOrderRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoryCaseAgentTest {

    @Test
    void shouldReturnSimilarCompletedRepairCasesWithoutPrivateVehicleIdentifiers() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        HistoryCaseAgent agent = new HistoryCaseAgent(repository, new PrivacyMaskingService());

        when(repository.findAllWithDetails()).thenReturn(List.of(
                completedOrder(
                        "RO-1001",
                        "\u6caaB67890 \u53d1\u52a8\u673a\u6020\u901f\u6296\u52a8\uff0c\u6545\u969c\u706f\u4eae\uff0cVIN LSVFA49J232123456\uff0c\u66f4\u6362\u706b\u82b1\u585e\u540e\u8def\u8bd5\u6b63\u5e38",
                        "\u5927\u4f17",
                        "\u9014\u89c2",
                        2021),
                pendingOrder(
                        "RO-1002",
                        "\u53d1\u52a8\u673a\u6296\u52a8\u5f85\u68c0\u6d4b",
                        "\u672c\u7530",
                        "\u96c5\u9601",
                        2020),
                completedOrder(
                        "RO-1003",
                        "\u7a7a\u8c03\u5236\u51b7\u4e0d\u8db3\uff0c\u8865\u5145\u51b7\u5a92",
                        "\u4e30\u7530",
                        "\u5361\u7f57\u62c9",
                        2019)));

        HistoryCaseAgent.HistoryCaseEvidence evidence = agent.analyze(
                "\u53d1\u52a8\u673a\u6296\u52a8\uff0c\u6545\u969c\u706f\u5076\u53d1\u70b9\u4eae");

        assertTrue(evidence.hasSimilarCases());
        assertTrue(evidence.summary().contains("History Case Agent"));
        assertTrue(evidence.summary().contains("RO-1001"));
        assertTrue(evidence.summary().contains("\u53d1\u52a8\u673a"));
        assertTrue(evidence.summary().contains("HISTORY_RELATED_CASE"));
        assertFalse(evidence.summary().contains("\u6caaB67890"));
        assertFalse(evidence.summary().contains("LSVFA49J232123456"));
        assertTrue(evidence.summary().contains("[MASKED_PLATE_1]"));
        assertTrue(evidence.summary().contains("[MASKED_VIN_1]"));
        assertFalse(evidence.summary().contains("RO-1002"));
        verify(repository).findAllWithDetails();
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void shouldReturnEmptySummaryWhenNoSimilarCasesFound() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        HistoryCaseAgent agent = new HistoryCaseAgent(repository, new PrivacyMaskingService());
        when(repository.findAllWithDetails()).thenReturn(List.of(completedOrder(
                "RO-2001",
                "\u7a7a\u8c03\u5236\u51b7\u5f02\u5e38",
                "\u4e30\u7530",
                "\u5361\u7f57\u62c9",
                2019)));

        HistoryCaseAgent.HistoryCaseEvidence evidence = agent.analyze("\u8f66\u95e8\u5bc6\u5c01\u6761\u8001\u5316");

        assertFalse(evidence.hasSimilarCases());
        assertTrue(evidence.summary().contains("HISTORY_NO_SIMILAR_CASE"));
    }

    @Test
    void shouldReturnLookupFailureEvidenceWhenHistoryReadFails() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        HistoryCaseAgent agent = new HistoryCaseAgent(repository, new PrivacyMaskingService());
        when(repository.findAllWithDetails()).thenThrow(new IllegalStateException("db unavailable"));

        HistoryCaseAgent.HistoryCaseEvidence evidence = agent.analyze("\u53d1\u52a8\u673a\u6296\u52a8");

        assertFalse(evidence.hasSimilarCases());
        assertTrue(evidence.summary().contains("HISTORY_LOOKUP_FAILED"));
    }

    private RepairOrder completedOrder(String orderNumber,
                                       String description,
                                       String brand,
                                       String model,
                                       int year) {
        RepairOrder order = repairOrder(orderNumber, description, brand, model, year);
        order.setStatus(RepairOrder.RepairStatus.COMPLETED);
        return order;
    }

    private RepairOrder pendingOrder(String orderNumber,
                                     String description,
                                     String brand,
                                     String model,
                                     int year) {
        RepairOrder order = repairOrder(orderNumber, description, brand, model, year);
        order.setStatus(RepairOrder.RepairStatus.PENDING);
        return order;
    }

    private RepairOrder repairOrder(String orderNumber,
                                    String description,
                                    String brand,
                                    String model,
                                    int year) {
        RepairOrder order = new RepairOrder();
        order.setOrderNumber(orderNumber);
        order.setDescription(description);
        order.setVehicle(vehicle(brand, model, year));
        order.setRepairType("repair");
        order.setActualHours(2.5);
        order.setMaterialCost(180.0);
        return order;
    }

    private Vehicle vehicle(String brand, String model, int year) {
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand(brand);
        vehicle.setModel(model);
        vehicle.setYear(year);
        vehicle.setLicensePlate("\u6caaB67890");
        vehicle.setVin("LSVFA49J232123456");
        return vehicle;
    }
}
