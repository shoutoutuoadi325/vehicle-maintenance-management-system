package org.com.repair.DTO;

public class AIDiagnosisResponse {
    private String faultType;
    private String suggestion;
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
