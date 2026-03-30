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

@Entity
@Table(name = "journey_completion_record")
public class JourneyCompletionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "grand_prize_granted", nullable = false)
    private Boolean grandPrizeGranted = false;

    @Column(name = "sticker_claimed", nullable = false)
    private Boolean stickerClaimed = false;

    @Column(name = "consignee_name", length = 100)
    private String consigneeName;

    @Column(name = "consignee_phone", length = 30)
    private String consigneePhone;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "shipping_status", nullable = false, length = 30)
    private String shippingStatus = "NOT_CLAIMED";

    @Column(name = "shipment_tracking_no", length = 80)
    private String shipmentTrackingNo;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Boolean getGrandPrizeGranted() {
        return grandPrizeGranted;
    }

    public void setGrandPrizeGranted(Boolean grandPrizeGranted) {
        this.grandPrizeGranted = grandPrizeGranted;
    }

    public Boolean getStickerClaimed() {
        return stickerClaimed;
    }

    public void setStickerClaimed(Boolean stickerClaimed) {
        this.stickerClaimed = stickerClaimed;
    }

    public String getConsigneeName() {
        return consigneeName;
    }

    public void setConsigneeName(String consigneeName) {
        this.consigneeName = consigneeName;
    }

    public String getConsigneePhone() {
        return consigneePhone;
    }

    public void setConsigneePhone(String consigneePhone) {
        this.consigneePhone = consigneePhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getShippingStatus() {
        return shippingStatus;
    }

    public void setShippingStatus(String shippingStatus) {
        this.shippingStatus = shippingStatus;
    }

    public String getShipmentTrackingNo() {
        return shipmentTrackingNo;
    }

    public void setShipmentTrackingNo(String shipmentTrackingNo) {
        this.shipmentTrackingNo = shipmentTrackingNo;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
