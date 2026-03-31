package org.com.repair.config;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String GAMIFICATION_RULES_CACHE = "gamificationRules";
    public static final String GAMIFICATION_LEADERBOARD_CACHE = "gamificationLeaderboard";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        Cache<Object, Object> rulesCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.SECONDS)
                .initialCapacity(32)
                .maximumSize(256)
                .build();
        cacheManager.registerCustomCache(GAMIFICATION_RULES_CACHE, Objects.requireNonNull(rulesCache));

        Cache<Object, Object> leaderboardCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .initialCapacity(16)
                .maximumSize(64)
                .build();
        cacheManager.registerCustomCache(GAMIFICATION_LEADERBOARD_CACHE, Objects.requireNonNull(leaderboardCache));

        return cacheManager;
    }
}