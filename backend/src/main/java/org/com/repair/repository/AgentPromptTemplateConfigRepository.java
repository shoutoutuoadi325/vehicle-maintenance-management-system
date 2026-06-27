package org.com.repair.repository;

import java.util.Optional;

import org.com.repair.entity.AgentPromptTemplateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentPromptTemplateConfigRepository extends JpaRepository<AgentPromptTemplateConfig, Long> {

    Optional<AgentPromptTemplateConfig> findByAgentRoleAndTemplateKeyAndEnabledTrue(
            String agentRole,
            String templateKey);

    Optional<AgentPromptTemplateConfig> findByAgentRoleAndTemplateKey(
            String agentRole,
            String templateKey);
}
