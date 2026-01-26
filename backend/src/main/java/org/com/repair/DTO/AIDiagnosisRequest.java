package org.com.repair.DTO;

import jakarta.validation.constraints.NotBlank;

public class AIDiagnosisRequest {
    @NotBlank(message = "问题描述不能为空")
    private String problemDescription;

    public AIDiagnosisRequest() {
    }

    public AIDiagnosisRequest(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }
}
