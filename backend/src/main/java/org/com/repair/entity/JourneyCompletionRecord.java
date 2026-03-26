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
    @Builder.Default
    private Boolean grandPrizeGranted = false;

    @Column(name = "sticker_claimed", nullable = false)
    @Builder.Default
    private Boolean stickerClaimed = false;

    @Column(name = "consignee_name", length = 100)
    private String consigneeName;

    @Column(name = "consignee_phone", length = 30)
    private String consigneePhone;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "shipping_status", nullable = false, length = 30)
    @Builder.Default
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
}
