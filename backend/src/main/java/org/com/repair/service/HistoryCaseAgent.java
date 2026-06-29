package org.com.repair.service;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.RepairOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class HistoryCaseAgent {

    private static final int MAX_CASES = 3;
    private static final double MIN_VECTOR_SIMILARITY = 0.18;

    private final RepairOrderRepository repairOrderRepository;
    private final PrivacyMaskingService privacyMaskingService;
    private final HistoryCaseVectorizer historyCaseVectorizer;

    public HistoryCaseAgent(RepairOrderRepository repairOrderRepository,
                            PrivacyMaskingService privacyMaskingService) {
        this(repairOrderRepository, privacyMaskingService, new HistoryCaseVectorizer());
    }

    @Autowired
    public HistoryCaseAgent(RepairOrderRepository repairOrderRepository,
                            PrivacyMaskingService privacyMaskingService,
                            HistoryCaseVectorizer historyCaseVectorizer) {
        this.repairOrderRepository = repairOrderRepository;
        this.privacyMaskingService = privacyMaskingService;
        this.historyCaseVectorizer = historyCaseVectorizer;
    }

    public HistoryCaseEvidence analyze(String problemDescription) {
        try {
            HistoryCaseVectorizer.VectorizedText queryVector = historyCaseVectorizer.vectorize(
                    privacyMaskingService.mask(problemDescription).maskedText());
            List<CaseMatch> matches = repairOrderRepository.findAllWithDetails().stream()
                    .filter(order -> RepairOrder.RepairStatus.COMPLETED.equals(order.getStatus()))
                    .map(order -> new CaseMatch(order, similarity(order, queryVector)))
                    .filter(match -> match.similarity() >= MIN_VECTOR_SIMILARITY)
                    .sorted(Comparator.comparingDouble(CaseMatch::similarity).reversed())
                    .limit(MAX_CASES)
                    .toList();

            if (matches.isEmpty()) {
                return new HistoryCaseEvidence(
                        "History Case Agent: RAG vector retrieval found no similar completed repair cases; tags=HISTORY_NO_SIMILAR_CASE",
                        List.of("HISTORY_NO_SIMILAR_CASE"));
            }

            List<String> riskTags = List.of("HISTORY_RELATED_CASE", "HISTORY_RAG_VECTOR_MATCH");
            return new HistoryCaseEvidence(buildSummary(matches, riskTags), riskTags);
        } catch (Exception ex) {
            return new HistoryCaseEvidence(
                    "History Case Agent: history lookup unavailable; tags=HISTORY_LOOKUP_FAILED",
                    List.of("HISTORY_LOOKUP_FAILED"));
        }
    }

    private double similarity(RepairOrder order, HistoryCaseVectorizer.VectorizedText queryVector) {
        String searchable = privacyMaskingService.mask(joinNonBlank(
                order.getDescription(),
                order.getOrderNumber(),
                order.getRepairType(),
                order.getRequiredSkillType() != null ? order.getRequiredSkillType().name() : "",
                vehicleText(order.getVehicle()))).maskedText();
        HistoryCaseVectorizer.VectorizedText caseVector = historyCaseVectorizer.vectorize(searchable);
        double vectorSimilarity = historyCaseVectorizer.cosineSimilarity(queryVector, caseVector);
        if (sharesDomainTerms(queryVector, caseVector)) {
            vectorSimilarity += 0.08;
        }
        return Math.min(1.0, vectorSimilarity);
    }

    private String buildSummary(List<CaseMatch> matches, List<String> riskTags) {
        StringBuilder summary = new StringBuilder("History Case Agent: RAG vector retrieval similar cases=");
        for (int i = 0; i < matches.size(); i++) {
            if (i > 0) {
                summary.append("; ");
            }
            RepairOrder order = matches.get(i).order();
            summary.append(caseSummary(order, matches.get(i).similarity()));
        }
        summary.append(". tags=").append(String.join(",", riskTags));
        return summary.toString();
    }

    private String caseSummary(RepairOrder order, double similarity) {
        StringBuilder builder = new StringBuilder();
        builder.append(nullToPlaceholder(order.getOrderNumber()))
                .append("(similarity=")
                .append(formatSimilarity(similarity))
                .append(", vehicle=")
                .append(publicVehicleLabel(order.getVehicle()))
                .append(", desc=")
                .append(safeDescription(order.getDescription()));

        if (order.getRepairType() != null && !order.getRepairType().isBlank()) {
            builder.append(", repairType=").append(order.getRepairType());
        }
        if (order.getActualHours() != null) {
            builder.append(", actualHours=").append(order.getActualHours());
        }
        if (order.getMaterialCost() != null) {
            builder.append(", materialCost=").append(order.getMaterialCost());
        }
        builder.append(")");
        return builder.toString();
    }

    private String formatSimilarity(double similarity) {
        return String.format(Locale.ROOT, "%.2f", similarity);
    }

    private String safeDescription(String description) {
        String masked = privacyMaskingService.mask(description).maskedText();
        if (masked.length() <= 80) {
            return nullToPlaceholder(masked);
        }
        return masked.substring(0, 80) + "...";
    }

    private String publicVehicleLabel(Vehicle vehicle) {
        if (vehicle == null) {
            return "unknown";
        }
        return joinNonBlank(
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear() == null ? "" : String.valueOf(vehicle.getYear()));
    }

    private String vehicleText(Vehicle vehicle) {
        if (vehicle == null) {
            return "";
        }
        return joinNonBlank(
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear() == null ? "" : String.valueOf(vehicle.getYear()));
    }

    private String joinNonBlank(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(" ");
                }
                builder.append(value.trim());
            }
        }
        return builder.toString();
    }

    private String nullToPlaceholder(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private boolean sharesDomainTerms(HistoryCaseVectorizer.VectorizedText queryVector,
                                      HistoryCaseVectorizer.VectorizedText caseVector) {
        List<String> queryTerms = queryVector == null ? List.of() : queryVector.terms();
        List<String> caseTerms = caseVector == null ? List.of() : caseVector.terms();
        for (String term : queryTerms) {
            if (term.length() >= 2 && caseTerms.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private record CaseMatch(RepairOrder order, double similarity) {
    }

    public record HistoryCaseEvidence(String summary, List<String> riskTags) {
        public boolean hasSimilarCases() {
            return riskTags != null && riskTags.contains("HISTORY_RELATED_CASE");
        }

        public String promptContext() {
            return "\n- " + summary;
        }
    }
}
