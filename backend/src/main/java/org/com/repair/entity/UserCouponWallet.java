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
@Table(name = "user_coupon_wallet")
public class UserCouponWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "brand_partner_id", nullable = false)
    private Long brandPartnerId;

    @Column(name = "city_index", nullable = false)
    private Integer cityIndex;

    @Column(name = "coupon_title", nullable = false, length = 255)
    private String couponTitle;

    @Column(name = "coupon_description", length = 1000)
    private String couponDescription;

    @Column(name = "coupon_status", nullable = false, length = 20)
    @Builder.Default
    private String couponStatus = "NEW";

    @Column(name = "source_action", nullable = false, length = 120)
    private String sourceAction;

    @Column(name = "draw_time", nullable = false)
    private LocalDateTime drawTime;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "redeem_time")
    private LocalDateTime redeemTime;

    @Column(name = "redeem_shop_id")
    private Long redeemShopId;

    @Column(name = "redeem_technician_id")
    private Long redeemTechnicianId;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }
}
