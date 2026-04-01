package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.com.repair.DTO.NewRepairOrderRequest;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.User;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.TechnicianRepository;
import org.com.repair.repository.UserRepository;
import org.com.repair.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

@SuppressWarnings("null")
class RepairOrderCreateValidationTest {

    private RepairOrderRepository repairOrderRepository;
    private UserRepository userRepository;
    private VehicleRepository vehicleRepository;
    private TechnicianRepository technicianRepository;
    private AutoAssignmentService autoAssignmentService;
    private EmissionCalculatorService emissionCalculatorService;
    private ApplicationEventPublisher eventPublisher;
    private RepairOrderService repairOrderService;

    @BeforeEach
    void setUp() {
        repairOrderRepository = mock(RepairOrderRepository.class);
        userRepository = mock(UserRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        autoAssignmentService = mock(AutoAssignmentService.class);
        emissionCalculatorService = mock(EmissionCalculatorService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        repairOrderService = new RepairOrderService(
                repairOrderRepository,
                userRepository,
                vehicleRepository,
                technicianRepository,
                autoAssignmentService,
                emissionCalculatorService,
                eventPublisher);
    }

    @Test
    void shouldRejectWhenVehicleAlreadyHasRepairOrder() {
        Long userId = 1L;
        Long vehicleId = 2L;

        User user = new User();
        user.setId(userId);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);

        NewRepairOrderRequest request = new NewRepairOrderRequest(
                null,
                null,
                "发动机异响",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                userId,
                vehicleId,
                null,
                null,
                null,
                null,
                null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(repairOrderRepository.existsByVehicleIdAndStatusIn(vehicleId,
            Set.of(
                RepairOrder.RepairStatus.PENDING,
                RepairOrder.RepairStatus.ASSIGNED,
                RepairOrder.RepairStatus.IN_PROGRESS)))
            .thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repairOrderService.createRepairOrder(request));

        assertTrue(exception.getMessage().contains("进行中"));
        verify(repairOrderRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

        @Test
        void shouldAllowWhenVehicleOnlyHasCompletedHistoryOrders() {
        Long userId = 1L;
        Long vehicleId = 2L;

        User user = new User();
        user.setId(userId);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);

        NewRepairOrderRequest request = new NewRepairOrderRequest(
            null,
            null,
            "历史已完成后再次报修",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            userId,
            vehicleId,
            null,
            null,
            null,
            null,
            null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(repairOrderRepository.existsByVehicleIdAndStatusIn(vehicleId,
            Set.of(
                RepairOrder.RepairStatus.PENDING,
                RepairOrder.RepairStatus.ASSIGNED,
                RepairOrder.RepairStatus.IN_PROGRESS)))
            .thenReturn(false);
        when(repairOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> repairOrderService.createRepairOrder(request));
        verify(repairOrderRepository).save(any());
        }
}
