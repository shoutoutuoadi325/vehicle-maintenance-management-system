package org.com.repair.DTO;

public class AIDiagnosisRequest {
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
