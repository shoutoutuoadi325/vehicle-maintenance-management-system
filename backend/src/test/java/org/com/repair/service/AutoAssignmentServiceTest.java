package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.com.repair.entity.DispatchWeightConfig;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Technician;
import org.com.repair.repository.DispatchWeightConfigRepository;
import org.com.repair.repository.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutoAssignmentServiceTest {

    private TechnicianRepository technicianRepository;
    private FeedbackService feedbackService;
    private DispatchWeightConfigRepository dispatchWeightConfigRepository;
    private TechnicianService technicianService;
    private AutoAssignmentService autoAssignmentService;

    @BeforeEach
    void setUp() {
        technicianRepository = mock(TechnicianRepository.class);
        feedbackService = mock(FeedbackService.class);
        dispatchWeightConfigRepository = mock(DispatchWeightConfigRepository.class);
        technicianService = mock(TechnicianService.class);
        autoAssignmentService = new AutoAssignmentService(
                technicianRepository,
                feedbackService,
                dispatchWeightConfigRepository,
                technicianService,
                new AgingAntiStarvationDispatchPolicy());
    }

    @Test
    void shouldApplyConfiguredFatiguePenaltyWhenSelectingTechnician() {
        Technician tiredTechnician = technician(1L, "高疲劳技师");
        Technician restedTechnician = technician(2L, "低疲劳技师");

        RepairOrder order = new RepairOrder();
        order.setId(10L);
        order.setRequiredSkillType(Technician.SkillType.MECHANIC);
        order.setDescription("发动机异响");
        order.setCreatedAt(new Date());
        order.setEstimatedHours(2.0);

        when(technicianRepository.findBySkillType(Technician.SkillType.MECHANIC))
                .thenReturn(List.of(tiredTechnician, restedTechnician));
        when(dispatchWeightConfigRepository.findByConfigKeyAndEnabledTrue("default"))
                .thenReturn(Optional.of(dispatchWeightConfig(
                        "0.5000",
                        "0.3000",
                        "0.2000",
                        "1.0000")));
        when(feedbackService.getAverageRatingByTechnicianId(1L)).thenReturn(5.0);
        when(feedbackService.getAverageRatingByTechnicianId(2L)).thenReturn(4.5);
        when(technicianService.getTechnicianFatigueSnapshot(1L))
                .thenReturn(new TechnicianService.TechnicianFatigueSnapshot(1.0, 8, 6.0, 3));
        when(technicianService.getTechnicianFatigueSnapshot(2L))
                .thenReturn(new TechnicianService.TechnicianFatigueSnapshot(0.0, 0, 0.0, 0));

        Technician assignedTechnician = autoAssignmentService.autoAssignBestTechnician(order);

        assertEquals(restedTechnician, assignedTechnician);
    }

    @Test
    void shouldRankOlderLongTailPendingOrdersFirstByAgingPolicy() {
        RepairOrder freshSimpleOrder = pendingOrder(
                1L,
                "RO-FRESH",
                "常规检查",
                1,
                1.0,
                Technician.SkillType.DIAGNOSTIC);
        RepairOrder olderLongTailOrder = pendingOrder(
                2L,
                "RO-OLDER",
                "复杂间歇异响排查",
                10,
                8.0,
                Technician.SkillType.MECHANIC);

        List<RepairOrder> rankedOrders = autoAssignmentService.rankPendingOrdersByAging(
                List.of(freshSimpleOrder, olderLongTailOrder));

        assertEquals(List.of(olderLongTailOrder, freshSimpleOrder), rankedOrders);
    }

    private Technician technician(Long id, String name) {
        Technician technician = new Technician();
        technician.setId(id);
        technician.setName(name);
        technician.setEmployeeId("EMP" + id);
        technician.setUsername("tech" + id);
        technician.setPassword("password");
        technician.setPhone("1380000000" + id);
        technician.setSkillType(Technician.SkillType.MECHANIC);
        technician.setHourlyRate(100.0);
        technician.setCompletedOrders(0);
        return technician;
    }

    private RepairOrder pendingOrder(Long id,
                                     String orderNumber,
                                     String description,
                                     int waitHours,
                                     double estimatedHours,
                                     Technician.SkillType skillType) {
        RepairOrder order = new RepairOrder();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setStatus(RepairOrder.RepairStatus.PENDING);
        order.setDescription(description);
        order.setCreatedAt(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(waitHours)));
        order.setEstimatedHours(estimatedHours);
        order.setRequiredSkillType(skillType);
        return order;
    }

    private DispatchWeightConfig dispatchWeightConfig(String ratingWeight,
                                                      String workloadWeight,
                                                      String experienceWeight,
                                                      String fatiguePenaltyWeight) {
        DispatchWeightConfig config = new DispatchWeightConfig();
        config.setConfigKey("default");
        config.setRatingWeight(new BigDecimal(ratingWeight));
        config.setWorkloadWeight(new BigDecimal(workloadWeight));
        config.setExperienceWeight(new BigDecimal(experienceWeight));
        config.setFatiguePenaltyWeight(new BigDecimal(fatiguePenaltyWeight));
        config.setEnabled(true);
        return config;
    }
}
