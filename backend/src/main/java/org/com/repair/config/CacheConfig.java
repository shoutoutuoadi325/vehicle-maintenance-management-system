package org.com.repair.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String GAMIFICATION_RULES_CACHE = "gamificationRules";
    public static final String GAMIFICATION_LEADERBOARD_CACHE = "gamificationLeaderboard";

    @Bean
    public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.registerCustomCache(
        GAMIFICATION_RULES_CACHE,
        Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .initialCapacity(32)
            .maximumSize(256)
            .build());
    cacheManager.registerCustomCache(
        GAMIFICATION_LEADERBOARD_CACHE,
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .initialCapacity(16)
            .maximumSize(64)
            .build());
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.SECONDS)
                .initialCapacity(32)
                .maximumSize(256));
        return cacheManager;
    }
}