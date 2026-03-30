package org.com.repair.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.com.repair.DTO.InventoryAlertPageResponse;
import org.com.repair.DTO.MaterialResponse;
import org.com.repair.DTO.MaintenanceAlertPageResponse;
import org.com.repair.DTO.NewMaterialRequest;
import org.com.repair.security.RequestUserContextResolver;
import org.com.repair.service.MaintenanceAlertService;
import org.com.repair.service.MaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AlertApiContractTest {

    private MockMvc mockMvc;
    private MaterialService materialService;
    private MaintenanceAlertService maintenanceAlertService;
    private RequestUserContextResolver requestUserContextResolver;

    @BeforeEach
    void setUp() {
        materialService = mock(MaterialService.class);
        maintenanceAlertService = mock(MaintenanceAlertService.class);
        requestUserContextResolver = mock(RequestUserContextResolver.class);

        MaterialController materialController = new MaterialController(materialService, requestUserContextResolver);
        MaintenanceAlertController maintenanceAlertController =
                new MaintenanceAlertController(maintenanceAlertService, requestUserContextResolver);

        mockMvc = MockMvcBuilders
                .standaloneSetup(materialController, maintenanceAlertController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnInventoryNotificationsPageWithGlobalSeverityCounters() throws Exception {
        when(materialService.getActiveInventoryAlertsPage(0, 10, "CRITICAL"))
                .thenReturn(new InventoryAlertPageResponse(0, 10, 5, 1, 3, 2, List.of()));

        mockMvc.perform(get("/api/materials/notifications/page")
                        .param("page", "0")
                        .param("size", "10")
                        .param("severity", "CRITICAL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.criticalCount").value(3))
                .andExpect(jsonPath("$.data.warningCount").value(2));

        verify(requestUserContextResolver).requireAdminRole(any());
    }

    @Test
    void shouldReturnNotFoundCodeWhenResolveSingleInventoryAlertMissing() throws Exception {
        when(materialService.resolveInventoryAlert(9L)).thenReturn(false);

        mockMvc.perform(put("/api/materials/notifications/9/resolve")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVENTORY_ALERT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorCode").value("INVENTORY_ALERT_NOT_FOUND"));
    }

    @Test
    void shouldMarkBatchMaintenanceAlertsRead() throws Exception {
        when(maintenanceAlertService.markReadBatch(eq(1L), anyList())).thenReturn(2);

        mockMvc.perform(put("/api/maintenance-alerts/user/1/read-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[11,12,12]}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.updated").value(2));

        verify(requestUserContextResolver).ensurePathUserMatch(any(), eq(1L));
    }

    @Test
    void shouldReturnAlertNotFoundCodeWhenMarkReadMissing() throws Exception {
        when(requestUserContextResolver.requireUserId(any())).thenReturn(1L);
        when(maintenanceAlertService.markRead(99L, 1L)).thenReturn(false);

        mockMvc.perform(put("/api/maintenance-alerts/99/read")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ALERT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorCode").value("ALERT_NOT_FOUND"));
    }

    @Test
    void shouldPassMaintenanceAlertsPageFilterArguments() throws Exception {
        when(maintenanceAlertService.getUserAlertsPage(1L, 1, 20, "UNREAD", "TIME_OVERDUE"))
                .thenReturn(new MaintenanceAlertPageResponse(1, 20, 0, 0, List.of()));

        mockMvc.perform(get("/api/maintenance-alerts/user/1/page")
                        .param("page", "1")
                        .param("size", "20")
                        .param("status", "UNREAD")
                        .param("alertType", "TIME_OVERDUE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));

        verify(requestUserContextResolver).ensurePathUserMatch(any(), eq(1L));
        verify(maintenanceAlertService).getUserAlertsPage(1L, 1, 20, "UNREAD", "TIME_OVERDUE");
    }

    @Test
    void shouldNormalizeMaintenancePaginationArguments() throws Exception {
        when(maintenanceAlertService.getUserAlertsPage(1L, 0, 50, null, null))
                .thenReturn(new MaintenanceAlertPageResponse(0, 50, 0, 0, List.of()));

        mockMvc.perform(get("/api/maintenance-alerts/user/1/page")
                        .param("page", "-3")
                        .param("size", "500")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(50));

        verify(maintenanceAlertService).getUserAlertsPage(1L, 0, 50, null, null);
    }

    @Test
    void shouldReturnBadRequestWhenMaintenanceStatusInvalid() throws Exception {
        mockMvc.perform(get("/api/maintenance-alerts/user/1/page")
                        .param("status", "NOT_A_STATUS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWhenMaintenanceAlertTypeInvalid() throws Exception {
        mockMvc.perform(get("/api/maintenance-alerts/user/1/page")
                        .param("alertType", "NOT_A_TYPE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void shouldResolveInventoryAlertsInBatch() throws Exception {
        when(materialService.resolveInventoryAlerts(anyList())).thenReturn(3);

        mockMvc.perform(put("/api/materials/notifications/resolve-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[21,22,23]}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.updated").value(3));

        verify(requestUserContextResolver).requireAdminRole(any());
    }

    @Test
    void shouldReturnBadRequestCodeWhenInventorySeverityInvalid() throws Exception {
        mockMvc.perform(get("/api/materials/notifications/page")
                        .param("page", "0")
                        .param("size", "10")
                        .param("severity", "INVALID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void shouldNormalizeInventoryPaginationArguments() throws Exception {
        when(materialService.getActiveInventoryAlertsPage(0, 50, null))
                .thenReturn(new InventoryAlertPageResponse(0, 50, 0, 0, 0, 0, List.of()));

        mockMvc.perform(get("/api/materials/notifications/page")
                        .param("page", "-99")
                        .param("size", "999")
                        .param("severity", "ALL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(50));

        verify(materialService).getActiveInventoryAlertsPage(0, 50, null);
    }

        @Test
        void shouldNormalizeInventoryPageSizeWhenZero() throws Exception {
                when(materialService.getActiveInventoryAlertsPage(0, 1, "WARNING"))
                                .thenReturn(new InventoryAlertPageResponse(0, 1, 0, 0, 0, 0, List.of()));

                mockMvc.perform(get("/api/materials/notifications/page")
                                                .param("page", "0")
                                                .param("size", "0")
                                                .param("severity", "warning")
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.size").value(1));

                verify(materialService).getActiveInventoryAlertsPage(0, 1, "WARNING");
        }

    @Test
    void shouldAcceptEmptyMaintenanceBatchPayload() throws Exception {
        when(maintenanceAlertService.markReadBatch(eq(1L), any())).thenReturn(0);

        mockMvc.perform(put("/api/maintenance-alerts/user/1/read-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.updated").value(0));

        verify(requestUserContextResolver).ensurePathUserMatch(any(), eq(1L));
    }

    @Test
    void shouldAcceptNullInventoryBatchPayload() throws Exception {
        when(materialService.resolveInventoryAlerts(any())).thenReturn(0);

        mockMvc.perform(put("/api/materials/notifications/resolve-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.updated").value(0));

        verify(requestUserContextResolver).requireAdminRole(any());
    }

    @Test
    void shouldReturnWrappedMaterialList() throws Exception {
        when(materialService.getAllMaterials()).thenReturn(List.of(new MaterialResponse(1L, "机油", 99.0, 20, 5)));

        mockMvc.perform(get("/api/materials").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].name").value("机油"));
    }

    @Test
    void shouldReturnNotFoundCodeWhenMaterialMissing() throws Exception {
        when(materialService.getMaterialById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/materials/404").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATERIAL_NOT_FOUND"))
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NOT_FOUND"));
    }

    @Test
    void shouldCreateMaterialWithCreatedResponse() throws Exception {
        when(materialService.addMaterial(any(NewMaterialRequest.class)))
                .thenReturn(new MaterialResponse(10L, "刹车片", 120.0, 30, 8));

        mockMvc.perform(post("/api/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"刹车片\",\"unitPrice\":120.0,\"stockQuantity\":30,\"minimumStockLevel\":8}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void shouldReturnBadRequestCodeWhenConsumeQuantityMissing() throws Exception {
        mockMvc.perform(put("/api/materials/1/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnSuccessWhenDeleteMaterial() throws Exception {
        when(materialService.deleteMaterial(7L)).thenReturn(true);

        mockMvc.perform(delete("/api/materials/7").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }
}
