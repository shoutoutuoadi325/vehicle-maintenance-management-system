package org.com.repair.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.com.repair.entity.GreenRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GreenRuleConfigRepository extends JpaRepository<GreenRuleConfig, Long> {

    Optional<GreenRuleConfig> findByRuleKeyAndEnabledTrue(String ruleKey);

    @Query("select max(c.updateTime) from GreenRuleConfig c where c.enabled = true")
    LocalDateTime findLatestEnabledUpdateTime();
}
