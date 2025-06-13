package org.com.repair.DTO;

import org.com.repair.entity.Technician.SkillType;

public record NewTechnicianRequest(
    String name,
    String employeeId,
    String username,
    String password,
    String phone,
    String email,
    SkillType skillType,
    Double hourlyRate
) {
} 