package org.com.repair.service;

import org.springframework.stereotype.Service;

@Service
public class SafetyNetGate {

    public static final String VALIDATED = "VALIDATED";
    public static final String SUSPENDED = "SUSPENDED";
    public static final double REVIEW_THRESHOLD = 0.85;

    public String evaluate(double confidence) {
        return confidence >= REVIEW_THRESHOLD ? VALIDATED : SUSPENDED;
    }
}
