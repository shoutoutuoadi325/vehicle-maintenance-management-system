package org.com.repair.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

@Entity
@Table(name = "technician_copilot_memory")
public class TechnicianCopilotMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "technician_id", nullable = false, unique = true)
    private Long technicianId;

    @Column(name = "diagnosis_count", nullable = false)
    private Integer diagnosisCount = 0;

    @Column(name = "last_problem_text", columnDefinition = "TEXT")
    private String lastProblemText;

    @Column(name = "last_fault_type", length = 255)
    private String lastFaultType;

    @Column(name = "last_suggestion", columnDefinition = "TEXT")
    private String lastSuggestion;

    @Column(name = "last_history_case_summary", columnDefinition = "TEXT")
    private String lastHistoryCaseSummary;

    @Column(name = "last_confidence")
    private Double lastConfidence;

    @Column(name = "last_workflow_status", length = 32)
    private String lastWorkflowStatus;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public Integer getDiagnosisCount() {
        return diagnosisCount;
    }

    public void setDiagnosisCount(Integer diagnosisCount) {
        this.diagnosisCount = diagnosisCount;
    }

    public String getLastProblemText() {
        return lastProblemText;
    }

    public void setLastProblemText(String lastProblemText) {
        this.lastProblemText = lastProblemText;
    }

    public String getLastFaultType() {
        return lastFaultType;
    }

    public void setLastFaultType(String lastFaultType) {
        this.lastFaultType = lastFaultType;
    }

    public String getLastSuggestion() {
        return lastSuggestion;
    }

    public void setLastSuggestion(String lastSuggestion) {
        this.lastSuggestion = lastSuggestion;
    }

    public String getLastHistoryCaseSummary() {
        return lastHistoryCaseSummary;
    }

    public void setLastHistoryCaseSummary(String lastHistoryCaseSummary) {
        this.lastHistoryCaseSummary = lastHistoryCaseSummary;
    }

    public Double getLastConfidence() {
        return lastConfidence;
    }

    public void setLastConfidence(Double lastConfidence) {
        this.lastConfidence = lastConfidence;
    }

    public String getLastWorkflowStatus() {
        return lastWorkflowStatus;
    }

    public void setLastWorkflowStatus(String lastWorkflowStatus) {
        this.lastWorkflowStatus = lastWorkflowStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
