package org.com.repair.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @BeforeEach
    void setUp() {
        AdminService adminService = mock(AdminService.class);
        repairOrderService = mock(RepairOrderService.class);
        TechnicianService technicianService = mock(TechnicianService.class);
        AuthTokenService authTokenService = mock(AuthTokenService.class);
        GamificationService gamificationService = mock(GamificationService.class);
        RequestUserContextResolver requestUserContextResolver = mock(RequestUserContextResolver.class);

        AdminController adminController = new AdminController(
                adminService,
                repairOrderService,
                technicianService,
                authTokenService,
                gamificationService,
                requestUserContextResolver);

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
}
