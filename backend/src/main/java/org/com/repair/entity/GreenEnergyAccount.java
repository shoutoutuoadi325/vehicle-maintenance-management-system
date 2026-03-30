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
import jakarta.persistence.Version;

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
    @Schema(description = "绿色能量余额", example = "120")
    private Integer totalEnergy = 0;

    @Column(name = "current_mileage", nullable = false)
    @Schema(description = "当前公路里程进度", example = "45")
    private Integer currentMileage = 0;

    @Column(name = "current_map_id", nullable = false)
    @Schema(description = "当前正在进行的路线ID", example = "1")
    private Long currentMapId = 1L;

    @Column(name = "journey_status", nullable = false, length = 40)
    @Schema(description = "旅程状态：NORMAL/PENDING_RANDOM_EVENT", example = "NORMAL")
    private String journeyStatus = "NORMAL";

    @Column(name = "pending_random_quiz_id")
    @Schema(description = "待完成的随机事件题目ID", example = "12")
    private Long pendingRandomQuizId;

    @Column(name = "frozen_mileage", nullable = false)
    @Schema(description = "冻结中的里程累计值", example = "40")
    private Integer frozenMileage = 0;

    @Column(name = "random_event_next_retry_time")
    @Schema(description = "随机事件下一次可重试时间")
    private LocalDateTime randomEventNextRetryTime;

    @Column(name = "update_time", nullable = false)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Version
    @Column(name = "version", nullable = false)
    @Schema(description = "乐观锁版本号", example = "0")
    private Long version = 0L;

    public GreenEnergyAccount() {
    }

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getTotalEnergy() {
        return totalEnergy;
    }

    public void setTotalEnergy(Integer totalEnergy) {
        this.totalEnergy = totalEnergy;
    }

    public Integer getCurrentMileage() {
        return currentMileage;
    }

    public void setCurrentMileage(Integer currentMileage) {
        this.currentMileage = currentMileage;
    }

    public Long getCurrentMapId() {
        return currentMapId;
    }

    public void setCurrentMapId(Long currentMapId) {
        this.currentMapId = currentMapId;
    }

    public String getJourneyStatus() {
        return journeyStatus;
    }

    public void setJourneyStatus(String journeyStatus) {
        this.journeyStatus = journeyStatus;
    }

    public Long getPendingRandomQuizId() {
        return pendingRandomQuizId;
    }

    public void setPendingRandomQuizId(Long pendingRandomQuizId) {
        this.pendingRandomQuizId = pendingRandomQuizId;
    }

    public Integer getFrozenMileage() {
        return frozenMileage;
    }

    public void setFrozenMileage(Integer frozenMileage) {
        this.frozenMileage = frozenMileage;
    }

    public LocalDateTime getRandomEventNextRetryTime() {
        return randomEventNextRetryTime;
    }

    public void setRandomEventNextRetryTime(LocalDateTime randomEventNextRetryTime) {
        this.randomEventNextRetryTime = randomEventNextRetryTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
