package org.com.repair.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "green_reward_ledger",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_green_reward_source_action", columnNames = {"source_type", "source_id", "action_key"})
        },
        indexes = {
                @Index(name = "idx_green_reward_user_time", columnList = "user_id,created_at")
        }
)
public class GreenRewardLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(name = "action_key", nullable = false, length = 60)
    private String actionKey;

    @Column(name = "energy_delta", nullable = false)
    private Integer energyDelta;

    @Column(name = "mileage_delta", nullable = false)
    private Integer mileageDelta;

    @Column(name = "risk_level", nullable = false, length = 20)
    @Builder.Default
    private String riskLevel = "LOW";

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
