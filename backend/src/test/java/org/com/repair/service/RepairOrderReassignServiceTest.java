package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.com.repair.DTO.RepairOrderResponse;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Technician;
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
class RepairOrderReassignServiceTest {

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
    void shouldReassignAndRefreshOldAndNewTechnicianLoad() {
        Long orderId = 100L;
        Technician oldTech = technician(1L, "T-OLD");
        Technician newTech = technician(2L, "T-NEW");

        RepairOrder order = new RepairOrder();
        order.setId(orderId);
        order.setOrderNumber("RO202601010001");
        order.setDescription("test");
        order.setStatus(RepairOrder.RepairStatus.ASSIGNED);
        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());
        order.setUser(new User());
        order.setVehicle(new Vehicle());
        order.setTechnicians(new HashSet<>(Set.of(oldTech)));

        when(repairOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(technicianRepository.findById(1L)).thenReturn(Optional.of(oldTech));
        when(technicianRepository.findById(2L)).thenReturn(Optional.of(newTech));
        when(repairOrderRepository.save(any(RepairOrder.class)))
            .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0, RepairOrder.class)));

        RepairOrder completed = new RepairOrder();
        completed.setStatus(RepairOrder.RepairStatus.COMPLETED);
        completed.setActualHours(2.5);
        RepairOrder inProgress = new RepairOrder();
        inProgress.setStatus(RepairOrder.RepairStatus.IN_PROGRESS);
        inProgress.setActualHours(null);

        when(repairOrderRepository.findByTechnicianId(1L)).thenReturn(List.of(completed));
        when(repairOrderRepository.findByTechnicianId(2L)).thenReturn(List.of(inProgress));

        RepairOrderResponse response = repairOrderService.reassignTechnicians(orderId, Set.of(2L), true);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        verify(technicianRepository, times(2)).save(org.mockito.ArgumentMatchers.isA(Technician.class));
        verify(repairOrderRepository).findByTechnicianId(eq(1L));
        verify(repairOrderRepository).findByTechnicianId(eq(2L));
    }

    private Technician technician(Long id, String employeeId) {
        Technician technician = new Technician();
        technician.setId(id);
        technician.setName(employeeId);
        technician.setEmployeeId(employeeId);
        technician.setUsername(employeeId.toLowerCase());
        technician.setPassword("pwd");
        technician.setPhone("13800000000");
        technician.setSkillType(Technician.SkillType.MECHANIC);
        technician.setHourlyRate(100.0);
        technician.setCompletedOrders(0);
        technician.setTotalWorkHours(0.0);
        return technician;
    }
}
