package org.com.repair.service;

import org.com.repair.DTO.MaterialResponse;
import org.com.repair.entity.InventoryAlertNotification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class InventoryDiagnosisAgent {

    private final MaterialService materialService;

    public InventoryDiagnosisAgent(MaterialService materialService) {
        this.materialService = materialService;
    }

    public InventoryEvidence analyze(String problemDescription) {
        try {
            List<String> keywords = resolveMaterialKeywords(problemDescription);
            List<MaterialResponse> relatedMaterials = materialService.getAllMaterials().stream()
                    .filter(material -> isRelated(material.name(), keywords, problemDescription))
                    .limit(5)
                    .toList();
            List<InventoryAlertNotification> relatedAlerts = materialService.getActiveInventoryAlerts().stream()
                    .filter(alert -> isRelated(alert.getMaterialName(), keywords, problemDescription))
                    .limit(5)
                    .toList();

            List<String> riskTags = new ArrayList<>();
            if (!relatedAlerts.isEmpty()) {
                riskTags.add("INVENTORY_LOW_STOCK");
            }
            if (!relatedMaterials.isEmpty()) {
                riskTags.add("INVENTORY_RELATED_MATERIAL");
            }
            if (relatedMaterials.isEmpty() && relatedAlerts.isEmpty()) {
                riskTags.add("INVENTORY_NO_RELATED_EVIDENCE");
            }

            return new InventoryEvidence(buildSummary(relatedMaterials, relatedAlerts, riskTags), riskTags);
        } catch (Exception ex) {
            return new InventoryEvidence(
                    "Inventory Agent: inventory lookup unavailable; tags=INVENTORY_LOOKUP_FAILED",
                    List.of("INVENTORY_LOOKUP_FAILED"));
        }
    }

    private boolean isRelated(String materialName, List<String> keywords, String problemDescription) {
        if (materialName == null || materialName.isBlank()) {
            return false;
        }
        String normalizedName = normalize(materialName);
        String normalizedProblem = normalize(problemDescription);
        if (!normalizedProblem.isBlank() && normalizedProblem.contains(normalizedName)) {
            return true;
        }
        for (String keyword : keywords) {
            if (normalizedName.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private List<String> resolveMaterialKeywords(String problemDescription) {
        String normalized = normalize(problemDescription);
        Set<String> keywords = new LinkedHashSet<>();

        if (containsAny(normalized, "\u5239\u8f66", "\u5236\u52a8", "\u5239\u8f66\u7247", "\u5239\u8f66\u76d8")) {
            addAll(keywords, "\u5239\u8f66\u7247", "\u5236\u52a8\u6db2", "\u5239\u8f66\u6cb9", "\u5239\u8f66\u76d8");
        }
        if (containsAny(normalized, "\u9ad8\u6e29", "\u6c34\u6e29", "\u51b7\u5374", "\u9632\u51bb")) {
            addAll(keywords, "\u51b7\u5374\u6db2", "\u9632\u51bb\u6db2", "\u6c34\u7bb1", "\u8282\u6e29\u5668");
        }
        if (containsAny(normalized, "\u6f0f\u6cb9", "\u673a\u6cb9", "\u6cb9\u6db2", "\u6cb9\u6f06")) {
            addAll(keywords, "\u673a\u6cb9", "\u6ee4\u82af", "\u5bc6\u5c01", "\u6cb9\u6f06", "\u8865\u6f06", "\u7802\u7eb8");
        }
        if (containsAny(normalized, "\u542f\u52a8", "\u7184\u706b", "\u6253\u4e0d\u7740", "\u7535\u74f6")) {
            addAll(keywords, "\u7535\u74f6", "\u84c4\u7535\u6c60", "\u706b\u82b1\u585e", "\u70b9\u706b\u7ebf\u5708");
        }
        if (containsAny(normalized, "\u7a7a\u8c03", "\u5236\u51b7", "\u51b7\u5a92")) {
            addAll(keywords, "\u7a7a\u8c03\u6ee4\u82af", "\u51b7\u5a92", "\u5236\u51b7\u5242");
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

    private String buildSummary(List<MaterialResponse> materials,
                                List<InventoryAlertNotification> alerts,
                                List<String> riskTags) {
        StringBuilder summary = new StringBuilder("Inventory Agent: ");
        if (materials.isEmpty() && alerts.isEmpty()) {
            return summary.append("no related inventory evidence; tags=")
                    .append(String.join(",", riskTags))
                    .toString();
        }

        if (!materials.isEmpty()) {
            summary.append("related materials=");
            for (int i = 0; i < materials.size(); i++) {
                if (i > 0) {
                    summary.append("; ");
                }
                MaterialResponse material = materials.get(i);
                summary.append(material.name())
                        .append("(stock=")
                        .append(nullToZero(material.stockQuantity()))
                        .append(", min=")
                        .append(nullToZero(material.minimumStockLevel()))
                        .append(")");
            }
            summary.append(". ");
        }

        if (!alerts.isEmpty()) {
            summary.append("active low-stock alerts=");
            for (int i = 0; i < alerts.size(); i++) {
                if (i > 0) {
                    summary.append("; ");
                }
                InventoryAlertNotification alert = alerts.get(i);
                summary.append(alert.getMaterialName())
                        .append("(stock=")
                        .append(nullToZero(alert.getCurrentStock()))
                        .append(", min=")
                        .append(nullToZero(alert.getMinimumStockLevel()))
                        .append(")");
            }
            summary.append(". ");
        }

        summary.append("tags=").append(String.join(",", riskTags));
        return summary.toString();
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    public record InventoryEvidence(String summary, List<String> riskTags) {
        public boolean hasLowStockRisk() {
            return riskTags != null && riskTags.contains("INVENTORY_LOW_STOCK");
        }

        public String promptContext() {
            return "\n- " + summary;
        }
    }
}
