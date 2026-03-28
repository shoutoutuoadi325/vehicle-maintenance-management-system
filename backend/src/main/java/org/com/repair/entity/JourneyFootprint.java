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
        name = "journey_footprint",
        indexes = {
                @Index(name = "idx_footprint_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_footprint_map_created", columnList = "map_id, created_at")
        }
)
public class JourneyFootprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "map_id", nullable = false)
    private Long mapId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "event_description", nullable = false, length = 500)
    private String eventDescription;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
