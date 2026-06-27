package org.com.repair.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.com.repair.service.FeedbackSelfIterationService;
import org.com.repair.security.RequestUserContextResolver;
import org.com.repair.service.AdminService;
import org.com.repair.service.AuthTokenService;
import org.com.repair.service.GamificationService;
import org.com.repair.service.RepairOrderService;
import org.com.repair.service.TechnicianService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminControllerResilienceTest {

    private MockMvc mockMvc;
    private RepairOrderService repairOrderService;
    private FeedbackSelfIterationService feedbackSelfIterationService;
    private RequestUserContextResolver requestUserContextResolver;

    @BeforeEach
    void setUp() {
        AdminService adminService = mock(AdminService.class);
        repairOrderService = mock(RepairOrderService.class);
        TechnicianService technicianService = mock(TechnicianService.class);
        AuthTokenService authTokenService = mock(AuthTokenService.class);
        GamificationService gamificationService = mock(GamificationService.class);
        requestUserContextResolver = mock(RequestUserContextResolver.class);
        feedbackSelfIterationService = mock(FeedbackSelfIterationService.class);

        AdminController adminController = new AdminController(
                adminService,
                repairOrderService,
                technicianService,
                authTokenService,
                gamificationService,
                requestUserContextResolver,
                feedbackSelfIterationService);

        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void shouldReturnZeroDashboardStatsWhenServiceThrows() throws Exception {
        when(repairOrderService.getAllRepairOrdersWithDetails())
                .thenThrow(new RuntimeException("simulated failure"));

        mockMvc.perform(get("/api/admins/dashboard-stats").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(0))
                .andExpect(jsonPath("$.pendingOrders").value(0))
                .andExpect(jsonPath("$.completedOrders").value(0))
                .andExpect(jsonPath("$.activeTechnicians").value(0));
    }

    @Test
    void shouldReturnInternalServerErrorForDetailedStatisticsWhenServiceThrows() throws Exception {
        when(repairOrderService.getDetailedStatistics("2026-01-01", "2026-01-31"))
                .thenThrow(new RuntimeException("simulated failure"));

        mockMvc.perform(get("/api/admins/detailed-statistics")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("获取统计数据失败: simulated failure"));
    }

    @Test
    void shouldGenerateSelfIterationDraftFromAdminEndpoint() throws Exception {
        FeedbackSelfIterationService.SelfIterationDraft draft = selfIterationDraft();
        when(feedbackSelfIterationService.buildIterationDraft()).thenReturn(draft);

        mockMvc.perform(post("/api/admins/ai-self-iteration/draft").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregate.totalFeedback").value(12))
                .andExpect(jsonPath("$.dispatchWeightDraft.ratingWeight").value(0.5500));

        verify(requestUserContextResolver).requireAdminRole(org.mockito.ArgumentMatchers.any());
    }

    private FeedbackSelfIterationService.SelfIterationDraft selfIterationDraft() {
        FeedbackSelfIterationService.FeedbackAggregate aggregate =
                new FeedbackSelfIterationService.FeedbackAggregate(30, 12, 3.6, 4, 0.3333, 3);
        FeedbackSelfIterationService.DispatchWeightDraft weightDraft =
                new FeedbackSelfIterationService.DispatchWeightDraft(
                        new BigDecimal("0.5500"),
                        new BigDecimal("0.2500"),
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.1000"),
                        "根据低分反馈占比生成派单权重调整建议。");
        return new FeedbackSelfIterationService.SelfIterationDraft(
                LocalDateTime.now(),
                aggregate,
                "prompt patch",
                weightDraft,
                List.of("UPDATE dispatch_weight_config ..."));
    }
}
