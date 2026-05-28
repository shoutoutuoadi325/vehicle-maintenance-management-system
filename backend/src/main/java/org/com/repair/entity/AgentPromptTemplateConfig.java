package org.com.repair.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Expert Agent prompt template configuration.
 * This table supports the "heavy decision, light training" feedback loop:
 * feedback samples are aggregated by SQL, then the system can produce a new prompt
 * template version for each expert Agent and persist the decision parameters for
 * subsequent diagnosis orchestration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "agent_prompt_template_config",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agent_prompt_template_role_key",
                columnNames = {"agent_role", "template_key"}))
public class AgentPromptTemplateConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_role", nullable = false, length = 64)
    private String agentRole;

    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;

    @Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "sample_window_days", nullable = false)
    @Builder.Default
    private Integer sampleWindowDays = 30;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "update_reason", length = 500)
    private String updateReason;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }
}
