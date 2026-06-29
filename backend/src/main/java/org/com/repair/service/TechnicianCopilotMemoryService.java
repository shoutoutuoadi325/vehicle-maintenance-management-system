package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.entity.TechnicianCopilotMemory;
import org.com.repair.repository.TechnicianCopilotMemoryRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class TechnicianCopilotMemoryService {

    private static final int MAX_PROBLEM_LENGTH = 600;
    private static final int MAX_SUGGESTION_LENGTH = 1200;
    private static final int MAX_HISTORY_LENGTH = 1200;

    private final TechnicianCopilotMemoryRepository technicianCopilotMemoryRepository;
    private final PrivacyMaskingService privacyMaskingService;

    public TechnicianCopilotMemoryService(TechnicianCopilotMemoryRepository technicianCopilotMemoryRepository,
                                          PrivacyMaskingService privacyMaskingService) {
        this.technicianCopilotMemoryRepository = technicianCopilotMemoryRepository;
        this.privacyMaskingService = privacyMaskingService;
    }

    public CopilotMemoryContext resolveContext(Long technicianId) {
        if (technicianId == null) {
            return CopilotMemoryContext.empty();
        }

        try {
            Optional<TechnicianCopilotMemory> memory = technicianCopilotMemoryRepository.findByTechnicianId(technicianId);
            if (memory.isEmpty()) {
                return new CopilotMemoryContext(
                        "Technician Copilot Memory: no prior persisted sessions for this technician.",
                        false);
            }
            TechnicianCopilotMemory value = memory.get();
            StringBuilder context = new StringBuilder("Technician Copilot Memory:");
            context.append("\n- priorSessions=").append(value.getDiagnosisCount() == null ? 0 : value.getDiagnosisCount());
            appendIfPresent(context, "lastProblem", value.getLastProblemText());
            appendIfPresent(context, "lastFaultType", value.getLastFaultType());
            appendIfPresent(context, "lastWorkflowStatus", value.getLastWorkflowStatus());
            if (value.getLastConfidence() != null) {
                context.append("\n- lastConfidence=").append(value.getLastConfidence());
            }
            appendIfPresent(context, "lastHistoryCaseEvidence", value.getLastHistoryCaseSummary());
            return new CopilotMemoryContext(context.toString(), true);
        } catch (Exception ex) {
            return new CopilotMemoryContext(
                    "Technician Copilot Memory: persisted memory lookup unavailable.",
                    false);
        }
    }

    public void recordInteraction(Long technicianId,
                                  String problemDescription,
                                  AIDiagnosisResponse response,
                                  HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence) {
        if (technicianId == null || response == null || !response.isSuccess()) {
            return;
        }

        try {
            TechnicianCopilotMemory memory = technicianCopilotMemoryRepository.findByTechnicianId(technicianId)
                    .orElseGet(TechnicianCopilotMemory::new);
            Date now = new Date();
            if (memory.getId() == null) {
                memory.setTechnicianId(technicianId);
                memory.setCreatedAt(now);
                memory.setDiagnosisCount(0);
            }

            memory.setDiagnosisCount((memory.getDiagnosisCount() == null ? 0 : memory.getDiagnosisCount()) + 1);
            memory.setLastProblemText(maskAndLimit(problemDescription, MAX_PROBLEM_LENGTH));
            memory.setLastFaultType(limit(response.getFaultType(), 255));
            memory.setLastSuggestion(maskAndLimit(response.getSuggestion(), MAX_SUGGESTION_LENGTH));
            memory.setLastHistoryCaseSummary(historyCaseEvidence == null
                    ? null
                    : maskAndLimit(historyCaseEvidence.summary(), MAX_HISTORY_LENGTH));
            memory.setLastConfidence(response.getConfidence());
            memory.setLastWorkflowStatus(limit(response.getWorkflowStatus(), 32));
            memory.setUpdatedAt(now);
            technicianCopilotMemoryRepository.save(memory);
        } catch (Exception ignored) {
            // Copilot memory is assistive context. Diagnosis must not fail because memory persistence is unavailable.
        }
    }

    private void appendIfPresent(StringBuilder context, String label, String value) {
        if (value != null && !value.isBlank()) {
            context.append("\n- ").append(label).append("=").append(value);
        }
    }

    private String maskAndLimit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return limit(privacyMaskingService.mask(value).maskedText(), maxLength);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    public record CopilotMemoryContext(String promptContext, boolean found) {
        public static CopilotMemoryContext empty() {
            return new CopilotMemoryContext("", false);
        }
    }
}
