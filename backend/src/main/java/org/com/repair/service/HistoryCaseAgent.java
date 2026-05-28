package org.com.repair.service;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.RepairOrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HistoryCaseAgent {

    private static final int MAX_CASES = 3;

    private final RepairOrderRepository repairOrderRepository;
    private final PrivacyMaskingService privacyMaskingService;

    public HistoryCaseAgent(RepairOrderRepository repairOrderRepository,
                            PrivacyMaskingService privacyMaskingService) {
        this.repairOrderRepository = repairOrderRepository;
        this.privacyMaskingService = privacyMaskingService;
    }

    public HistoryCaseEvidence analyze(String problemDescription) {
        try {
            List<String> keywords = resolveKeywords(problemDescription);
            List<CaseMatch> matches = repairOrderRepository.findAllWithDetails().stream()
                    .filter(order -> RepairOrder.RepairStatus.COMPLETED.equals(order.getStatus()))
                    .map(order -> new CaseMatch(order, score(order, keywords, problemDescription)))
                    .filter(match -> match.score() > 0)
                    .sorted(Comparator.comparingInt(CaseMatch::score).reversed())
                    .limit(MAX_CASES)
                    .toList();

            if (matches.isEmpty()) {
                return new HistoryCaseEvidence(
                        "History Case Agent: no similar completed repair cases; tags=HISTORY_NO_SIMILAR_CASE",
                        List.of("HISTORY_NO_SIMILAR_CASE"));
            }

            List<String> riskTags = List.of("HISTORY_RELATED_CASE");
            return new HistoryCaseEvidence(buildSummary(matches, riskTags), riskTags);
        } catch (Exception ex) {
            return new HistoryCaseEvidence(
                    "History Case Agent: history lookup unavailable; tags=HISTORY_LOOKUP_FAILED",
                    List.of("HISTORY_LOOKUP_FAILED"));
        }
    }

    private int score(RepairOrder order, List<String> keywords, String problemDescription) {
        String searchable = normalize(joinNonBlank(
                order.getDescription(),
                order.getOrderNumber(),
                order.getRepairType(),
                order.getRequiredSkillType() != null ? order.getRequiredSkillType().name() : "",
                vehicleText(order.getVehicle())));
        if (searchable.isBlank()) {
            return 0;
        }

        int score = 0;
        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            if (!normalizedKeyword.isBlank() && searchable.contains(normalizedKeyword)) {
                score += 2;
            }
        }

        String normalizedProblem = normalize(problemDescription);
        if (!normalizedProblem.isBlank() && searchable.contains(normalizedProblem)) {
            score += 3;
        }
        return score;
    }

    private String buildSummary(List<CaseMatch> matches, List<String> riskTags) {
        StringBuilder summary = new StringBuilder("History Case Agent: similar cases=");
        for (int i = 0; i < matches.size(); i++) {
            if (i > 0) {
                summary.append("; ");
            }
            RepairOrder order = matches.get(i).order();
            summary.append(caseSummary(order, matches.get(i).score()));
        }
        summary.append(". tags=").append(String.join(",", riskTags));
        return summary.toString();
    }

    private String caseSummary(RepairOrder order, int score) {
        StringBuilder builder = new StringBuilder();
        builder.append(nullToPlaceholder(order.getOrderNumber()))
                .append("(score=")
                .append(score)
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

    private List<String> resolveKeywords(String problemDescription) {
        String normalized = normalize(problemDescription);
        Set<String> keywords = new LinkedHashSet<>();

        if (containsAny(normalized, "\u53d1\u52a8\u673a", "\u6296\u52a8", "\u6545\u969c\u706f", "\u6020\u901f", "\u7184\u706b")) {
            addAll(keywords,
                    "\u53d1\u52a8\u673a",
                    "\u6296\u52a8",
                    "\u6545\u969c\u706f",
                    "\u6020\u901f",
                    "\u706b\u82b1\u585e",
                    "\u70b9\u706b",
                    "\u8fdb\u6c14",
                    "\u7184\u706b");
        }
        if (containsAny(normalized, "\u5239\u8f66", "\u5236\u52a8", "\u5239\u8f66\u7247", "\u5239\u8f66\u76d8")) {
            addAll(keywords,
                    "\u5239\u8f66",
                    "\u5236\u52a8",
                    "\u5239\u8f66\u7247",
                    "\u5239\u8f66\u76d8",
                    "\u5f02\u54cd");
        }
        if (containsAny(normalized, "\u9ad8\u6e29", "\u6c34\u6e29", "\u51b7\u5374", "\u9632\u51bb")) {
            addAll(keywords,
                    "\u9ad8\u6e29",
                    "\u6c34\u6e29",
                    "\u51b7\u5374",
                    "\u51b7\u5374\u6db2",
                    "\u6c34\u7bb1");
        }
        if (containsAny(normalized, "\u6f0f\u6cb9", "\u673a\u6cb9", "\u6cb9\u6db2", "\u6cb9\u6f06", "\u6f06\u9762")) {
            addAll(keywords,
                    "\u6f0f\u6cb9",
                    "\u673a\u6cb9",
                    "\u6cb9\u6db2",
                    "\u6cb9\u6f06",
                    "\u6f06\u9762",
                    "\u8865\u6f06");
        }
        if (containsAny(normalized, "\u542f\u52a8", "\u6253\u4e0d\u7740", "\u7535\u74f6", "\u84c4\u7535\u6c60")) {
            addAll(keywords,
                    "\u542f\u52a8",
                    "\u6253\u4e0d\u7740",
                    "\u7535\u74f6",
                    "\u84c4\u7535\u6c60",
                    "\u70b9\u706b");
        }
        if (containsAny(normalized, "\u7a7a\u8c03", "\u5236\u51b7", "\u51b7\u5a92")) {
            addAll(keywords,
                    "\u7a7a\u8c03",
                    "\u5236\u51b7",
                    "\u51b7\u5a92",
                    "\u7a7a\u8c03\u6ee4\u82af");
        }

        if (keywords.isEmpty() && !normalized.isBlank()) {
            keywords.add(problemDescription);
        }
        return new ArrayList<>(keywords);
    }

    private void addAll(Set<String> target, String... values) {
        target.addAll(List.of(values));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
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

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private record CaseMatch(RepairOrder order, int score) {
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
