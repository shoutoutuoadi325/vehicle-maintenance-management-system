package org.com.repair.controller;

import java.util.Objects;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.com.repair.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class VehicleControllerExceptionMappingTest {

    private MockMvc mockMvc;
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = mock(VehicleService.class);
        VehicleController vehicleController = new VehicleController(vehicleService);
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleController).build();
    }

    @Test
    void shouldReturnConflictWhenBusinessStateConflictOccurs() throws Exception {
        when(vehicleService.addVehicle(any())).thenThrow(new IllegalStateException("车牌号已存在"));

        mockMvc.perform(post("/api/vehicles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(validVehiclePayload())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("车牌号已存在"));
    }

    @Test
    void shouldReturnBadRequestWhenBusinessArgumentInvalid() throws Exception {
        when(vehicleService.addVehicle(any())).thenThrow(new IllegalArgumentException("用户不存在"));

        mockMvc.perform(post("/api/vehicles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(validVehiclePayload())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    private String validVehiclePayload() {
        return """
                {
                  "licensePlate": "A-12345",
                  "brand": "Brand",
                  "model": "Model",
                  "year": 2023,
                  "color": "Black",
                  "vin": "VIN123",
                  "userId": 1,
                  "currentMileage": 1000,
                  "lastMaintenanceMileage": 100,
                  "lastMaintenanceAt": null
                }
                """;
    }
}
