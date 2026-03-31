package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.com.repair.DTO.VehicleResponse;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.User;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.UserRepository;
import org.com.repair.repository.VehicleRepository;
import org.junit.jupiter.api.Test;

class VehicleServiceBatchFetchTest {

    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RepairOrderRepository repairOrderRepository = mock(RepairOrderRepository.class);

    private final VehicleService vehicleService = new VehicleService(
            vehicleRepository,
            userRepository,
            repairOrderRepository);

    @Test
    void shouldBatchLoadRepairOrdersForUserVehicles() {
        Vehicle vehicleA = vehicle(1L, "A-100");
        Vehicle vehicleB = vehicle(2L, "B-200");

        when(vehicleRepository.findByUserId(10L)).thenReturn(List.of(vehicleA, vehicleB));
        when(repairOrderRepository.findByVehicleIdIn(List.of(1L, 2L))).thenReturn(List.of(
                order(101L, vehicleA),
                order(102L, vehicleA),
                order(201L, vehicleB)));

        List<VehicleResponse> responses = vehicleService.getVehiclesByUserId(10L);

        assertEquals(2, responses.size());
        assertEquals(2, responses.get(0).repairOrders().size());
        assertEquals(1, responses.get(1).repairOrders().size());
        verify(repairOrderRepository, times(1)).findByVehicleIdIn(List.of(1L, 2L));
    }

    @Test
    void shouldReturnEmptyWithoutQueryingOrdersWhenNoVehicles() {
        when(vehicleRepository.findByUserId(20L)).thenReturn(List.of());

        List<VehicleResponse> responses = vehicleService.getVehiclesByUserId(20L);

        assertTrue(responses.isEmpty());
        verify(repairOrderRepository, times(0)).findByVehicleIdIn(anyList());
    }

    @Test
    void shouldFailFastWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () -> vehicleService.getVehiclesByUserId(null));
        verify(vehicleRepository, never()).findByUserId(null);
    }

    @Test
    void shouldSkipBatchOrderQueryWhenVehicleIdsAreMissing() {
        Vehicle vehicleWithoutId = vehicle(null, "N-000");
        when(vehicleRepository.findByUserId(30L)).thenReturn(List.of(vehicleWithoutId));

        List<VehicleResponse> responses = vehicleService.getVehiclesByUserId(30L);

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).repairOrders().isEmpty());
        verify(repairOrderRepository, never()).findByVehicleIdIn(anyList());
    }

    private Vehicle vehicle(Long id, String plate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setLicensePlate(plate);
        vehicle.setBrand("Brand");
        vehicle.setModel("Model");

        User user = new User();
        user.setId(10L);
        vehicle.setUser(user);
        return vehicle;
    }

    private RepairOrder order(Long id, Vehicle vehicle) {
        RepairOrder order = new RepairOrder();
        order.setId(id);
        order.setOrderNumber("ORD-" + id);
        order.setDescription("desc");
        order.setStatus(RepairOrder.RepairStatus.PENDING);
        order.setVehicle(vehicle);
        return order;
    }
}