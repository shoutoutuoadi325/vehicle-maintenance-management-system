package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.com.repair.entity.RepairOrder;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.TechnicianRepository;
import org.junit.jupiter.api.Test;

class TechnicianServiceFatigueTest {

    private final TechnicianRepository technicianRepository = mock(TechnicianRepository.class);
    private final RepairOrderRepository repairOrderRepository = mock(RepairOrderRepository.class);
    private final FeedbackService feedbackService = mock(FeedbackService.class);
    private final AutoAssignmentService autoAssignmentService = mock(AutoAssignmentService.class);

    private final TechnicianService technicianService = new TechnicianService(
            technicianRepository,
            repairOrderRepository,
            feedbackService,
            autoAssignmentService);

    @Test
    void shouldReturnBaselineFatigueWhenNoWorkToday() {
        when(repairOrderRepository.findByTechnicianId(1L)).thenReturn(List.of());

        TechnicianService.TechnicianFatigueSnapshot snapshot = technicianService.getTechnicianFatigueSnapshot(1L);

        assertEquals(0.1, snapshot.getFatigueLevel(), 0.0001);
        assertEquals(0, snapshot.getCompletedToday());
        assertEquals(0.0, snapshot.getContinuousWorkingHours(), 0.0001);
        assertEquals(0, snapshot.getNightOrdersToday());
    }

    @Test
    void shouldCalculateFatigueFromCompletedContinuousAndNightFactors() {
        LocalDate today = LocalDate.now();

        RepairOrder dayOrder1 = order(
                RepairOrder.RepairStatus.COMPLETED,
                today.atTime(8, 0),
                today.atTime(10, 0));

        RepairOrder dayOrder2 = order(
                RepairOrder.RepairStatus.COMPLETED,
                today.atTime(10, 20),
                today.atTime(12, 0));

        RepairOrder nightOrder = order(
                RepairOrder.RepairStatus.COMPLETED,
                today.atTime(22, 30),
                today.atTime(23, 30));

        when(repairOrderRepository.findByTechnicianId(2L)).thenReturn(List.of(dayOrder1, dayOrder2, nightOrder));

        TechnicianService.TechnicianFatigueSnapshot snapshot = technicianService.getTechnicianFatigueSnapshot(2L);

        assertEquals(3, snapshot.getCompletedToday());
        assertEquals(4.0, snapshot.getContinuousWorkingHours(), 0.01);
        assertEquals(1, snapshot.getNightOrdersToday());

        double expected = 0.1 + (3.0 / 8.0) * 0.4 + (4.0 / 6.0) * 0.35 + (1.0 / 3.0) * 0.15;
        assertEquals(expected, snapshot.getFatigueLevel(), 0.02);
    }

    @Test
    void shouldCapFatigueAtOneForExtremeLoad() {
        LocalDate today = LocalDate.now();
        // Use a full-day interval to keep this assertion deterministic even right after midnight.
        RepairOrder longShift = order(
            RepairOrder.RepairStatus.COMPLETED,
            today.atStartOfDay(),
            today.plusDays(1).atStartOfDay());

        List<RepairOrder> heavyOrders = List.of(
                longShift,
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(1, 0), today.atTime(2, 0)),
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(3, 0), today.atTime(4, 0)),
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(5, 0), today.atTime(6, 0)),
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(7, 0), today.atTime(8, 0)),
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(9, 0), today.atTime(10, 0)),
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(11, 0), today.atTime(12, 0)),
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(13, 0), today.atTime(14, 0)),
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(15, 0), today.atTime(16, 0)),
                order(RepairOrder.RepairStatus.COMPLETED, today.atTime(22, 0), today.atTime(23, 0)));

        when(repairOrderRepository.findByTechnicianId(3L)).thenReturn(heavyOrders);

        TechnicianService.TechnicianFatigueSnapshot snapshot = technicianService.getTechnicianFatigueSnapshot(3L);

        assertTrue(snapshot.getFatigueLevel() >= 0.99 && snapshot.getFatigueLevel() <= 1.0);
    }

    private RepairOrder order(RepairOrder.RepairStatus status, LocalDateTime start, LocalDateTime end) {
        RepairOrder order = new RepairOrder();
        order.setStatus(status);

        Date startDate = toDate(start);
        order.setCreatedAt(startDate);
        order.setStartedAt(startDate);
        order.setUpdatedAt(startDate);

        if (end != null) {
            Date endDate = toDate(end);
            order.setCompletedAt(endDate);
            order.setUpdatedAt(endDate);
        }
        return order;
    }

    private Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}
