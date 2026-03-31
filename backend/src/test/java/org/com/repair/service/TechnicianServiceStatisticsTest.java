package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.com.repair.controller.TechnicianController.TechnicianStatistics;
import org.com.repair.entity.RepairOrder;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.TechnicianRepository;
import org.junit.jupiter.api.Test;

class TechnicianServiceStatisticsTest {

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
    void shouldCountCompletedAndPendingTasksByEnumStatus() {
        when(repairOrderRepository.findByTechnicianId(7L)).thenReturn(List.of(
                order(RepairOrder.RepairStatus.COMPLETED),
                order(RepairOrder.RepairStatus.ASSIGNED),
                order(RepairOrder.RepairStatus.IN_PROGRESS),
                order(RepairOrder.RepairStatus.CANCELLED)));
        when(feedbackService.getAverageRatingByTechnicianId(7L)).thenReturn(4.8);
        when(technicianRepository.calculateTotalEarnings(7L)).thenReturn(1000.0);

        LocalDate today = LocalDate.now();
        when(technicianRepository.calculateMonthlyEarnings(7L, today.getYear(), today.getMonthValue()))
                .thenReturn(220.0);

        TechnicianStatistics stats = technicianService.getTechnicianStatistics(7L);

        assertEquals(4, stats.getTotalTasks());
        assertEquals(1, stats.getCompletedTasks());
        assertEquals(2, stats.getPendingTasks());
        assertEquals(4.8, stats.getAverageRating(), 0.0001);
        assertEquals(1000.0, stats.getTotalEarnings(), 0.0001);
        assertEquals(220.0, stats.getMonthlyEarnings(), 0.0001);
    }

    @Test
    void shouldFallbackToZeroWhenRatingAndEarningsAreNull() {
        when(repairOrderRepository.findByTechnicianId(8L)).thenReturn(List.of());
        when(feedbackService.getAverageRatingByTechnicianId(8L)).thenReturn(null);
        when(technicianRepository.calculateTotalEarnings(8L)).thenReturn(null);

        LocalDate today = LocalDate.now();
        when(technicianRepository.calculateMonthlyEarnings(8L, today.getYear(), today.getMonthValue()))
                .thenReturn(0.0);

        TechnicianStatistics stats = technicianService.getTechnicianStatistics(8L);

        assertEquals(0, stats.getTotalTasks());
        assertEquals(0, stats.getCompletedTasks());
        assertEquals(0, stats.getPendingTasks());
        assertEquals(0.0, stats.getAverageRating(), 0.0001);
        assertEquals(0.0, stats.getTotalEarnings(), 0.0001);
        assertEquals(0.0, stats.getMonthlyEarnings(), 0.0001);
    }

    private RepairOrder order(RepairOrder.RepairStatus status) {
        RepairOrder order = new RepairOrder();
        order.setStatus(status);
        return order;
    }
}
