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

@Entity
@Table(
        name = "journey_map_node",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_journey_map_node", columnNames = {"map_id", "city_index"})
        },
        indexes = {
                @Index(name = "idx_journey_map_node_map", columnList = "map_id")
        }
)
public class JourneyMapNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "map_id", nullable = false)
    private Long mapId;

    @Column(name = "city_index", nullable = false)
    private Integer cityIndex;

    @Column(name = "city_name", nullable = false, length = 120)
    private String cityName;

    @Column(name = "required_mileage", nullable = false)
    private Integer requiredMileage;

    @Column(name = "x", nullable = false)
    private Integer x;

    @Column(name = "y", nullable = false)
    private Integer y;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getMapId() { return mapId; }
        public void setMapId(Long mapId) { this.mapId = mapId; }
        public Integer getCityIndex() { return cityIndex; }
        public void setCityIndex(Integer cityIndex) { this.cityIndex = cityIndex; }
        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }
        public Integer getRequiredMileage() { return requiredMileage; }
        public void setRequiredMileage(Integer requiredMileage) { this.requiredMileage = requiredMileage; }
        public Integer getX() { return x; }
        public void setX(Integer x) { this.x = x; }
        public Integer getY() { return y; }
        public void setY(Integer y) { this.y = y; }
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
