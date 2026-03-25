package org.com.repair.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.com.repair.config.CacheConfig;
import org.com.repair.entity.GreenRuleConfig;
import org.com.repair.repository.GreenRuleConfigRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GamificationRuleService {

    private final GreenRuleConfigRepository greenRuleConfigRepository;

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
        return getEnabledRuleMap().getOrDefault(key, defaultValue);
    }

    @Cacheable(cacheNames = CacheConfig.GAMIFICATION_RULES_CACHE, key = "'enabledRuleMap'")
    public Map<String, String> getEnabledRuleMap() {
        return greenRuleConfigRepository.findAll().stream()
                .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .collect(Collectors.toUnmodifiableMap(
                        GreenRuleConfig::getRuleKey,
                        GreenRuleConfig::getRuleValue,
                        (left, right) -> right));
    }

    @CacheEvict(cacheNames = CacheConfig.GAMIFICATION_RULES_CACHE, allEntries = true)
    public void evictRuleCache() {
        // Used by management flows that update green_rule_config.
    }
}
