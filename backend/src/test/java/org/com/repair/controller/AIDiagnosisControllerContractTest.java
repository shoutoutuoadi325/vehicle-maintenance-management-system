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
