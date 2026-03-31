package org.com.repair.DTO;

import jakarta.validation.constraints.NotBlank;

public class AIDiagnosisRequest {
    @NotBlank(message = "问题描述不能为空")
    private String problemDescription;
    private String role = "customer";
    private Long technicianId;

    public AIDiagnosisRequest() {
    }

    public AIDiagnosisRequest(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public AIDiagnosisRequest(String problemDescription, String role) {
        this.problemDescription = problemDescription;
        this.role = role;
    }

    public AIDiagnosisRequest(String problemDescription, String role, Long technicianId) {
        this.problemDescription = problemDescription;
        this.role = role;
        this.technicianId = technicianId;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }
}
