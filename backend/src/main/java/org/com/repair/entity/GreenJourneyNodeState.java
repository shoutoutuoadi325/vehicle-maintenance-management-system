package org.com.repair.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
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
        name = "green_journey_node_state",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_green_journey_user_city", columnNames = {"user_id", "city_index"})
        },
        indexes = {
                @Index(name = "idx_green_journey_user", columnList = "user_id")
        }
)
public class GreenJourneyNodeState {

    public enum NodeState {
        LOCKED,
        UNLOCKED,
        CHECKED_IN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "city_index", nullable = false)
    private Integer cityIndex;

    @Column(name = "node_state", nullable = false, length = 20)
    private String nodeState;

    @Column(name = "checkin_at")
    private LocalDateTime checkinAt;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }
}
