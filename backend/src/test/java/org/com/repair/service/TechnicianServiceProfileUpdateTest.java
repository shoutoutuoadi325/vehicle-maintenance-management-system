package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.com.repair.DTO.NewTechnicianRequest;
import org.com.repair.DTO.TechnicianResponse;
import org.com.repair.entity.Technician;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.TechnicianRepository;
import org.junit.jupiter.api.Test;

class TechnicianServiceProfileUpdateTest {

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
    void shouldUpdateEditableTechnicianProfileFields() {
        Technician technician = technician();
        when(technicianRepository.findById(7L)).thenReturn(Optional.of(technician));
        when(technicianRepository.save(any(Technician.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TechnicianResponse response = technicianService.updateTechnician(7L, new NewTechnicianRequest(
                "李浩",
                "EMP007",
                "tech7",
                "",
                "13800000007",
                "tech7@example.com",
                Technician.SkillType.DIAGNOSTIC,
                128.0,
                4.7,
                "新能源,疑难诊断,OBD"));

        assertEquals(Technician.SkillType.DIAGNOSTIC, response.skillType());
        assertEquals(128.0, response.hourlyRate(), 0.0001);
        assertEquals(4.7, response.serviceRating(), 0.0001);
        assertEquals("新能源,疑难诊断,OBD", response.skillTags());
    }

    private Technician technician() {
        Technician technician = new Technician();
        technician.setId(7L);
        technician.setName("李浩");
        technician.setEmployeeId("EMP007");
        technician.setUsername("tech7");
        technician.setPassword("password");
        technician.setPhone("13800000007");
        technician.setEmail("old@example.com");
        technician.setSkillType(Technician.SkillType.MECHANIC);
        technician.setHourlyRate(90.0);
        return technician;
    }
}
