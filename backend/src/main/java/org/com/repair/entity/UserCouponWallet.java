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
import jakarta.persistence.Version;

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

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public Long getBrandPartnerId() { return brandPartnerId; }
    public void setBrandPartnerId(Long brandPartnerId) { this.brandPartnerId = brandPartnerId; }
    public Integer getCityIndex() { return cityIndex; }
    public void setCityIndex(Integer cityIndex) { this.cityIndex = cityIndex; }
    public String getCouponTitle() { return couponTitle; }
    public void setCouponTitle(String couponTitle) { this.couponTitle = couponTitle; }
    public String getCouponDescription() { return couponDescription; }
    public void setCouponDescription(String couponDescription) { this.couponDescription = couponDescription; }
    public String getCouponStatus() { return couponStatus; }
    public void setCouponStatus(String couponStatus) { this.couponStatus = couponStatus; }
    public String getSourceAction() { return sourceAction; }
    public void setSourceAction(String sourceAction) { this.sourceAction = sourceAction; }
    public LocalDateTime getDrawTime() { return drawTime; }
    public void setDrawTime(LocalDateTime drawTime) { this.drawTime = drawTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getRedeemTime() { return redeemTime; }
    public void setRedeemTime(LocalDateTime redeemTime) { this.redeemTime = redeemTime; }
    public Long getRedeemShopId() { return redeemShopId; }
    public void setRedeemShopId(Long redeemShopId) { this.redeemShopId = redeemShopId; }
    public Long getRedeemTechnicianId() { return redeemTechnicianId; }
    public void setRedeemTechnicianId(Long redeemTechnicianId) { this.redeemTechnicianId = redeemTechnicianId; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
