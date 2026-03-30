package org.com.repair.DTO;

public class AIDiagnosisResponse {
    private String faultType;
    private String suggestion;
    private String severityLevel;
    private java.util.List<String> possibleCauses = new java.util.ArrayList<>();
    private Integer estimatedCostMin;
    private Integer estimatedCostMax;
    private Integer estimatedHoursMin;
    private Integer estimatedHoursMax;
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

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public java.util.List<String> getPossibleCauses() {
        return possibleCauses;
    }

    public void setPossibleCauses(java.util.List<String> possibleCauses) {
        this.possibleCauses = possibleCauses;
    }

    public Integer getEstimatedCostMin() {
        return estimatedCostMin;
    }

    public void setEstimatedCostMin(Integer estimatedCostMin) {
        this.estimatedCostMin = estimatedCostMin;
    }

    public Integer getEstimatedCostMax() {
        return estimatedCostMax;
    }

    public void setEstimatedCostMax(Integer estimatedCostMax) {
        this.estimatedCostMax = estimatedCostMax;
    }

    public Integer getEstimatedHoursMin() {
        return estimatedHoursMin;
    }

    public void setEstimatedHoursMin(Integer estimatedHoursMin) {
        this.estimatedHoursMin = estimatedHoursMin;
    }

    public Integer getEstimatedHoursMax() {
        return estimatedHoursMax;
    }

    public void setEstimatedHoursMax(Integer estimatedHoursMax) {
        this.estimatedHoursMax = estimatedHoursMax;
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
