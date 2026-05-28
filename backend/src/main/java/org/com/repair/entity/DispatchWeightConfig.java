package org.com.repair.entity;

import java.math.BigDecimal;
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

/**
 * Dispatch scoring weight configuration.
 * SQL aggregates feedback, workload, experience and fatigue samples, then stores
 * the adjusted scoring weights here as part of the no-retraining self-iteration loop.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dispatch_weight_config")
public class DispatchWeightConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "rating_weight", nullable = false, precision = 6, scale = 4)
    private BigDecimal ratingWeight;

    @Column(name = "workload_weight", nullable = false, precision = 6, scale = 4)
    private BigDecimal workloadWeight;

    @Column(name = "experience_weight", nullable = false, precision = 6, scale = 4)
    private BigDecimal experienceWeight;

    @Column(name = "fatigue_penalty_weight", nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal fatiguePenaltyWeight = BigDecimal.ZERO;

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
