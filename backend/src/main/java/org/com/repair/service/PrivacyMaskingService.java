package org.com.repair.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PrivacyMaskingService {

    private static final Pattern VIN_PATTERN = Pattern.compile("\\b[A-HJ-NPR-Z0-9]{17}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LICENSE_PLATE_PATTERN = Pattern.compile(
            "(?<![A-Z0-9\\u4e00-\\u9fa5])(?:[\\u4e00-\\u9fa5][A-Z][A-Z0-9]{5,6})(?![A-Z0-9])",
            Pattern.CASE_INSENSITIVE);

    public MaskingResult mask(String text) {
        String source = text == null ? "" : text;
        MaskingState vinState = replaceMatches(source, VIN_PATTERN, "MASKED_VIN");
        MaskingState plateState = replaceMatches(vinState.text(), LICENSE_PLATE_PATTERN, "MASKED_PLATE");
        return new MaskingResult(
                source,
                plateState.text(),
                vinState.count(),
                plateState.count());
    }

    private MaskingState replaceMatches(String source, Pattern pattern, String label) {
        Matcher matcher = pattern.matcher(source);
        StringBuffer buffer = new StringBuffer();
        int count = 0;
        while (matcher.find()) {
            count++;
            matcher.appendReplacement(buffer, "[" + label + "_" + count + "]");
        }
        matcher.appendTail(buffer);
        return new MaskingState(buffer.toString(), count);
    }

    public record MaskingResult(String originalText,
                                String maskedText,
                                int vinCount,
                                int licensePlateCount) {

        public boolean changed() {
            return vinCount > 0 || licensePlateCount > 0;
        }
    }

    private record MaskingState(String text, int count) {
    }
}
