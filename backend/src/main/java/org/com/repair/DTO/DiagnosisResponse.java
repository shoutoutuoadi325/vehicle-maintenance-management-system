package org.com.repair.DTO;

import java.util.List;

public record DiagnosisResponse(
    String faultType,
    String possibleCause,
    List<String> recommendedActions,
    String estimatedSeverity,
    Double estimatedCost,
    String skillTypeRequired
) {}
