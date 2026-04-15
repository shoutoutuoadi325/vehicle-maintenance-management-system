package org.com.repair.DTO;

import java.util.ArrayList;
import java.util.List;

public class AIDiagnosisRequest {
    private String problemDescription;
    private String role = "customer";
    private Long technicianId;
    private List<String> imageDataUrls = new ArrayList<>();

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

    public AIDiagnosisRequest(String problemDescription,
                              String role,
                              Long technicianId,
                              List<String> imageDataUrls) {
        this.problemDescription = problemDescription;
        this.role = role;
        this.technicianId = technicianId;
        this.imageDataUrls = imageDataUrls;
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

    public List<String> getImageDataUrls() {
        return imageDataUrls;
    }

    public void setImageDataUrls(List<String> imageDataUrls) {
        this.imageDataUrls = imageDataUrls;
    }
}
