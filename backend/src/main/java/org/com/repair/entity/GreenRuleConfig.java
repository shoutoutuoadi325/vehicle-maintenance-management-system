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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "green_rule_config")
public class GreenRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_key", nullable = false, unique = true, length = 100)
    private String ruleKey;

    @Column(name = "rule_value", nullable = false, length = 255)
    private String ruleValue;

    @Column(name = "value_type", nullable = false, length = 20)
    @Builder.Default
    private String valueType = "STRING";

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }
}
