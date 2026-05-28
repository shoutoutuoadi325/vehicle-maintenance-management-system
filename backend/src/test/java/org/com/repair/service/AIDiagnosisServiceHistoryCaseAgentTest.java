package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.RepairOrderRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIDiagnosisServiceHistoryCaseAgentTest {

    @Test
    void technicianSemanticPromptShouldIncludeHistoryCaseEvidence() {
        RepairOrderRepository repository = historyRepositoryWithEngineCase();
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService(repository);

        AIDiagnosisResponse response = service.diagnoseFault(
                "\u6020\u901f\u5076\u53d1\u4e0d\u987a\uff0c\u6000\u7591\u70b9\u706b\u6548\u7387\u4e0b\u964d",
                "technician",
                7L);

        assertTrue(service.capturedPrompt.contains("History Case Agent context"));
        assertTrue(service.capturedPrompt.contains("RO-HIS-1"));
        assertTrue(service.capturedPrompt.contains("HISTORY_RELATED_CASE"));
        assertFalse(service.capturedPrompt.contains("\u6caaB67890"));
        assertFalse(service.capturedPrompt.contains("LSVFA49J232123456"));
        assertTrue(response.getDecisionPath().stream()
                .anyMatch(step -> step.contains("History Case Agent") && step.contains("HISTORY_RELATED_CASE")));
        verify(repository).findAllWithDetails();
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void customerDiagnosisShouldNotCallHistoryCaseAgent() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        CapturingAIDiagnosisService service = new CapturingAIDiagnosisService(repository);

        AIDiagnosisResponse response = service.diagnoseFault(
                "\u6020\u901f\u5076\u53d1\u4e0d\u987a\uff0c\u6000\u7591\u70b9\u706b\u6548\u7387\u4e0b\u964d",
                "customer",
                null);

        assertFalse(service.capturedPrompt.contains("History Case Agent context"));
        assertTrue(response.getDecisionPath().isEmpty());
        verify(repository, never()).findAllWithDetails();
    }

    private RepairOrderRepository historyRepositoryWithEngineCase() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = new RepairOrder();
        order.setOrderNumber("RO-HIS-1");
        order.setStatus(RepairOrder.RepairStatus.COMPLETED);
        order.setDescription("\u6caaB67890 \u53d1\u52a8\u673a\u6296\u52a8\uff0c\u6545\u969c\u706f\u4eae\uff0cVIN LSVFA49J232123456\uff0c\u66f4\u6362\u706b\u82b1\u585e\u5e76\u6e05\u6d17\u8282\u6c14\u95e8\u540e\u6062\u590d");
        order.setVehicle(vehicle());
        order.setRepairType("repair");
        order.setActualHours(2.0);
        order.setMaterialCost(220.0);
        when(repository.findAllWithDetails()).thenReturn(List.of(order));
        return repository;
    }

    private Vehicle vehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand("\u5927\u4f17");
        vehicle.setModel("\u9014\u89c2");
        vehicle.setYear(2021);
        vehicle.setLicensePlate("\u6caaB67890");
        vehicle.setVin("LSVFA49J232123456");
        return vehicle;
    }

    private static class CapturingAIDiagnosisService extends AIDiagnosisService {
        private String capturedPrompt = "";

        private CapturingAIDiagnosisService(RepairOrderRepository repository) {
            super(
                    mock(GamificationService.class),
                    mock(TechnicianService.class),
                    new RuleDiagnosisService(),
                    new PrivacyMaskingService(),
                    new SemanticDiagnosisAgent(),
                    new InventoryDiagnosisAgent(materialServiceWithNoEvidence()),
                    new HistoryCaseAgent(repository, new PrivacyMaskingService()),
                    new DecisionFusionEngine(new SafetyNetGate()));
        }

        @Override
        protected String callOpenAIAPI(String prompt, String traceId, String stage) throws IOException {
            this.capturedPrompt = prompt;
            return """
                    {
                      "faultType": "engine vibration",
                      "suggestion": "read OBD codes, inspect spark plugs, ignition coils and throttle body",
                      "possibleCauses": ["spark plug wear", "throttle body carbon deposit"]
                    }
                    """;
        }

        private static MaterialService materialServiceWithNoEvidence() {
            MaterialService materialService = mock(MaterialService.class);
            when(materialService.getAllMaterials()).thenReturn(List.of());
            when(materialService.getActiveInventoryAlerts()).thenReturn(List.of());
            return materialService;
        }
    }
}
