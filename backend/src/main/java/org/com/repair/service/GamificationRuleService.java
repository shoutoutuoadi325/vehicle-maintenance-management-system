package org.com.repair.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.com.repair.entity.GreenRuleConfig;
import org.com.repair.repository.GreenRuleConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class GamificationRuleService {

    private static final long CACHE_TTL_SECONDS = 15;

    private final GreenRuleConfigRepository greenRuleConfigRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile LocalDateTime latestDbUpdateTime = LocalDateTime.MIN;
    private volatile long cacheRefreshedAtEpoch = 0L;

    public GamificationRuleService(GreenRuleConfigRepository greenRuleConfigRepository) {
        this.greenRuleConfigRepository = greenRuleConfigRepository;
    }

    public double getDouble(String key, double defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getString(String key, String defaultValue) {
        refreshCacheIfNeeded();
        return cache.getOrDefault(key, defaultValue);
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if ((now - cacheRefreshedAtEpoch) < CACHE_TTL_SECONDS * 1000) {
            return;
        }

        synchronized (this) {
            now = System.currentTimeMillis();
            if ((now - cacheRefreshedAtEpoch) < CACHE_TTL_SECONDS * 1000) {
                return;
            }

            LocalDateTime latest = greenRuleConfigRepository.findLatestEnabledUpdateTime();
            if (latest != null && !latest.isAfter(latestDbUpdateTime)) {
                cacheRefreshedAtEpoch = now;
                return;
            }

            for (GreenRuleConfig config : greenRuleConfigRepository.findAll()) {
                if (Boolean.TRUE.equals(config.getEnabled())) {
                    cache.put(config.getRuleKey(), config.getRuleValue());
                }
            }

            latestDbUpdateTime = latest == null ? LocalDateTime.now() : latest;
            cacheRefreshedAtEpoch = now;
        }
    }
}
