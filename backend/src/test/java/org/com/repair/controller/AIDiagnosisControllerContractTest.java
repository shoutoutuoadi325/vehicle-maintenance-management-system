package org.com.repair.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Objects;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.security.RequestUserContextResolver;
import org.com.repair.service.AIDiagnosisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AIDiagnosisControllerContractTest {

    private MockMvc mockMvc;
    private AIDiagnosisService aiDiagnosisService;
    private RequestUserContextResolver requestUserContextResolver;

    @BeforeEach
    void setUp() {
        aiDiagnosisService = mock(AIDiagnosisService.class);
        requestUserContextResolver = mock(RequestUserContextResolver.class);

        AIDiagnosisController controller = new AIDiagnosisController(aiDiagnosisService, requestUserContextResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void shouldUseCustomerContextRoleForSelfDiagnosis() throws Exception {
        when(requestUserContextResolver.requireRole(any())).thenReturn("customer");
        when(aiDiagnosisService.diagnoseFault(eq("发动机异响"), eq("customer"), eq(null), eq(List.of())))
                .thenReturn(new AIDiagnosisResponse("疑似积碳", "建议先做基础检查"));

        mockMvc.perform(post("/api/ai-diagnosis/diagnose")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"problemDescription\":\"发动机异响\",\"role\":\"admin\"}"))
                .andExpect(status().isOk());

        verify(aiDiagnosisService).diagnoseFault("发动机异响", "customer", null, List.of());
    }

    @Test
    void shouldUseTechnicianContextRoleForProfessionalDiagnosis() throws Exception {
        when(requestUserContextResolver.requireRole(any())).thenReturn("admin");
        when(aiDiagnosisService.diagnoseFault(eq("刹车偏软"), eq("technician"), eq(7L), eq(List.of())))
                .thenReturn(new AIDiagnosisResponse("制动液异常", "建议尽快复检"));

        mockMvc.perform(post("/api/ai-diagnosis/diagnose")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"problemDescription\":\"刹车偏软\",\"technicianId\":7}"))
                .andExpect(status().isOk());

        verify(aiDiagnosisService).diagnoseFault("刹车偏软", "technician", 7L, List.of());
    }

    @Test
    void shouldForwardImagePayloadToDiagnosisService() throws Exception {
        when(requestUserContextResolver.requireRole(any())).thenReturn("technician");
        when(aiDiagnosisService.diagnoseFault(eq("发动机舱有焦糊味"), eq("technician"), eq(12L), anyList()))
                .thenReturn(new AIDiagnosisResponse("线路疑似过热", "请先断电并检查线束"));

        mockMvc.perform(post("/api/ai-diagnosis/diagnose")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("""
                        {
                          "problemDescription": "发动机舱有焦糊味",
                          "technicianId": 12,
                          "imageDataUrls": ["data:image/png;base64,abc123"]
                        }
                        """))
                .andExpect(status().isOk());

        verify(aiDiagnosisService).diagnoseFault(
                "发动机舱有焦糊味",
                "technician",
                12L,
                List.of("data:image/png;base64,abc123"));
    }

    @Test
    void shouldAllowImageOnlyPayloadFromFrontendEntries() throws Exception {
        when(requestUserContextResolver.requireRole(any())).thenReturn("customer");
        when(aiDiagnosisService.diagnoseFault(eq(""), eq("customer"), eq(null), anyList()))
                .thenReturn(new AIDiagnosisResponse("visual precheck", "inspect visible damage and add text if needed"));

        mockMvc.perform(post("/api/ai-diagnosis/diagnose")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("""
                        {
                          "problemDescription": "   ",
                          "imageDataUrls": [
                            " data:image/png;base64,front1 ",
                            "",
                            "data:image/jpeg;base64,front2"
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faultType").value("visual precheck"))
                .andExpect(jsonPath("$.suggestion").value("inspect visible damage and add text if needed"));

        verify(aiDiagnosisService).diagnoseFault(
                "",
                "customer",
                null,
                List.of("data:image/png;base64,front1", "data:image/jpeg;base64,front2"));
    }

    @Test
    void shouldSerializeLegacyAndTechnicianEvidenceFields() throws Exception {
        when(requestUserContextResolver.requireRole(any())).thenReturn("technician");

        AIDiagnosisResponse response = new AIDiagnosisResponse("brake system", "check brake fluid first");
        response.setSeverityLevel("HIGH");
        response.setPossibleCauses(List.of("low brake fluid"));
        response.setEstimatedCostMin(120);
        response.setEstimatedCostMax(480);
        response.setEstimatedHoursMin(1);
        response.setEstimatedHoursMax(3);
        response.setConfidence(0.91);
        response.setWorkflowStatus("VALIDATED");
        response.setRuleHits(List.of("brake-fluid-low"));
        response.setAgentSummaries(List.of("Inventory Agent: brake fluid available"));
        response.setInventoryWarnings(List.of("brake fluid当前库存低于安全库存：当前 1，安全库存 3，请先确认备件。"));
        response.setDecisionPath(List.of("Decision Fusion: confidence=0.91"));

        when(aiDiagnosisService.diagnoseFault(eq("brake pedal feels soft"), eq("technician"), eq(7L), eq(List.of())))
                .thenReturn(response);

        mockMvc.perform(post("/api/ai-diagnosis/diagnose")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"problemDescription\":\"brake pedal feels soft\",\"technicianId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faultType").value("brake system"))
                .andExpect(jsonPath("$.suggestion").value("check brake fluid first"))
                .andExpect(jsonPath("$.severityLevel").value("HIGH"))
                .andExpect(jsonPath("$.possibleCauses[0]").value("low brake fluid"))
                .andExpect(jsonPath("$.estimatedCostMin").value(120))
                .andExpect(jsonPath("$.estimatedCostMax").value(480))
                .andExpect(jsonPath("$.estimatedHoursMin").value(1))
                .andExpect(jsonPath("$.estimatedHoursMax").value(3))
                .andExpect(jsonPath("$.confidence").value(0.91))
                .andExpect(jsonPath("$.workflowStatus").value("VALIDATED"))
                .andExpect(jsonPath("$.ruleHits[0]").value("brake-fluid-low"))
                .andExpect(jsonPath("$.agentSummaries[0]").value("Inventory Agent: brake fluid available"))
                .andExpect(jsonPath("$.inventoryWarnings[0]").value("brake fluid当前库存低于安全库存：当前 1，安全库存 3，请先确认备件。"))
                .andExpect(jsonPath("$.decisionPath[0]").value("Decision Fusion: confidence=0.91"));
    }

    @Test
    void shouldKeepLegacyDiagnosisEndpointAliasCompatible() throws Exception {
        when(requestUserContextResolver.requireRole(any())).thenReturn("customer");
        when(aiDiagnosisService.diagnoseFault(eq("engine noise"), eq("customer"), eq(null), eq(List.of())))
                .thenReturn(new AIDiagnosisResponse("engine noise", "book a basic inspection"));

        mockMvc.perform(post("/api/diagnosis")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"problemDescription\":\"engine noise\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faultType").value("engine noise"))
                .andExpect(jsonPath("$.suggestion").value("book a basic inspection"));

        verify(aiDiagnosisService).diagnoseFault("engine noise", "customer", null, List.of());
    }

    @Test
    void shouldReturn400WhenTextAndImageAreBothMissing() throws Exception {
        when(requestUserContextResolver.requireRole(any())).thenReturn("customer");

        mockMvc.perform(post("/api/ai-diagnosis/diagnose")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"problemDescription\":\"\",\"imageDataUrls\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("API_400_BAD_REQUEST"));

        verifyNoInteractions(aiDiagnosisService);
    }

    @Test
    void shouldReturn403WhenRoleIsNotAllowedForDiagnosis() throws Exception {
        when(requestUserContextResolver.requireRole(any())).thenReturn("guest");

        mockMvc.perform(post("/api/ai-diagnosis/diagnose")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"problemDescription\":\"电瓶亏电\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("API_403_FORBIDDEN"));
    }
}
