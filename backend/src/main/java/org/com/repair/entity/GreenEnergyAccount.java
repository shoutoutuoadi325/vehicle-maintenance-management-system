package org.com.repair.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Table(name = "green_energy_account")
@Schema(description = "用户绿色能量账户")
public class GreenEnergyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    @Schema(description = "用户ID（与User表单向关联）", example = "1001")
    private Long userId;

    @Column(name = "total_energy", nullable = false)
    @Builder.Default
    @Schema(description = "绿色能量余额", example = "120")
    private Integer totalEnergy = 0;

    @Column(name = "current_mileage", nullable = false)
    @Builder.Default
    @Schema(description = "当前公路里程进度", example = "45")
    private Integer currentMileage = 0;

    @Column(name = "update_time", nullable = false)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }
}
