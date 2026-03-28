package org.com.repair.DTO;

import java.util.List;

public class AIDiagnosisResponse {
    private String faultType;
    private String suggestion;
    private List<String> possibleCauses;
    private String severityLevel;
    private String estimatedCost;
    private String estimatedTime;
    private boolean success;
    private String errorMessage;

    public AIDiagnosisResponse() {
    }

    public AIDiagnosisResponse(String faultType, String suggestion) {
        this.faultType = faultType;
        this.suggestion = suggestion;
        this.success = true;
    }

    public AIDiagnosisResponse(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
    }

    public String getFaultType() {
        return faultType;
    }

    public void setFaultType(String faultType) {
        this.faultType = faultType;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public List<String> getPossibleCauses() {
        return possibleCauses;
    }

    public void setPossibleCauses(List<String> possibleCauses) {
        this.possibleCauses = possibleCauses;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public String getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(String estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
