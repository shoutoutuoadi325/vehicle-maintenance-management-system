package org.com.repair.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HistoryCaseVectorizer {

    private static final int VECTOR_DIMENSIONS = 256;

    private static final List<String> DOMAIN_TERMS = List.of(
            "发动机", "抖动", "故障灯", "怠速", "熄火", "火花塞", "点火", "点火线圈", "节气门", "进气",
            "刹车", "制动", "刹车片", "刹车盘", "制动液", "异响", "偏软",
            "高温", "水温", "冷却", "冷却液", "防冻液", "水箱", "漏液",
            "启动", "打不着", "电瓶", "蓄电池", "发电机",
            "空调", "制冷", "冷媒", "滤芯", "异味",
            "漏油", "机油", "变速箱油", "油液", "漆面", "补漆",
            "高压", "电机", "电控", "电池", "新能源"
    );

    private static final List<SynonymRule> SYNONYM_RULES = List.of(
            new SynonymRule(List.of("打不着", "无法启动", "启动困难"), "启动"),
            new SynonymRule(List.of("点火效率", "缺缸", "失火"), "火花塞"),
            new SynonymRule(List.of("顿挫", "喘振", "怠速不稳"), "抖动"),
            new SynonymRule(List.of("仪表灯", "故障码", "obd", "p0301"), "故障灯"),
            new SynonymRule(List.of("不制冷", "冷风弱"), "制冷"),
            new SynonymRule(List.of("刹不住", "刹车变软", "刹车偏软"), "制动"),
            new SynonymRule(List.of("水温高", "开锅"), "高温")
    );

    public VectorizedText vectorize(String text) {
        List<String> terms = extractTerms(text);
        double[] values = new double[VECTOR_DIMENSIONS];
        for (String term : terms) {
            int hash = stableHash(term);
            int index = Math.floorMod(hash, VECTOR_DIMENSIONS);
            double weight = resolveWeight(term);
            values[index] += ((hash & 1) == 0 ? weight : -weight);
        }

        double norm = 0.0;
        for (double value : values) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm > 0.0) {
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i] / norm;
            }
        }
        return new VectorizedText(values, terms);
    }

    public double cosineSimilarity(VectorizedText left, VectorizedText right) {
        if (left == null || right == null) {
            return 0.0;
        }
        double[] leftValues = left.values();
        double[] rightValues = right.values();
        int length = Math.min(leftValues.length, rightValues.length);
        double dot = 0.0;
        for (int i = 0; i < length; i++) {
            dot += leftValues[i] * rightValues[i];
        }
        return Math.max(0.0, dot);
    }

    public List<String> extractTerms(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }

        Set<String> terms = new LinkedHashSet<>();
        for (String domainTerm : DOMAIN_TERMS) {
            if (normalized.contains(normalize(domainTerm))) {
                terms.add(domainTerm);
            }
        }
        for (SynonymRule rule : SYNONYM_RULES) {
            for (String alias : rule.aliases()) {
                if (normalized.contains(normalize(alias))) {
                    terms.add(rule.canonicalTerm());
                    terms.add(alias);
                }
            }
        }

        addLatinTokens(normalized, terms);
        addCjkNgrams(normalized, terms, 2);
        addCjkNgrams(normalized, terms, 3);
        return new ArrayList<>(terms);
    }

    private void addLatinTokens(String normalized, Set<String> terms) {
        String[] tokens = normalized.split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() >= 2) {
                terms.add(token);
            }
        }
    }

    private void addCjkNgrams(String normalized, Set<String> terms, int ngramSize) {
        List<Integer> cjkCodePoints = normalized.codePoints()
                .filter(this::isCjk)
                .boxed()
                .toList();
        if (cjkCodePoints.size() < ngramSize) {
            return;
        }

        for (int i = 0; i <= cjkCodePoints.size() - ngramSize; i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < ngramSize; j++) {
                builder.appendCodePoint(cjkCodePoints.get(i + j));
            }
            terms.add(builder.toString());
        }
    }

    private boolean isCjk(int codePoint) {
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                || (codePoint >= 0x3400 && codePoint <= 0x4DBF);
    }

    private double resolveWeight(String term) {
        return DOMAIN_TERMS.contains(term) ? 2.0 : 1.0;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private int stableHash(String term) {
        byte[] bytes = term.getBytes(StandardCharsets.UTF_8);
        int hash = 0x811c9dc5;
        for (byte item : bytes) {
            hash ^= item & 0xff;
            hash *= 0x01000193;
        }
        return hash;
    }

    public record VectorizedText(double[] values, List<String> terms) {
    }

    private record SynonymRule(List<String> aliases, String canonicalTerm) {
    }
}
