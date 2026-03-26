package org.com.repair.entity;

import java.math.BigDecimal;
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
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_partner_id", nullable = false)
    private Long brandPartnerId;

    @Column(name = "city_index", nullable = false)
    private Integer cityIndex;

    @Column(name = "coupon_title", nullable = false, length = 255)
    private String couponTitle;

    @Column(name = "coupon_description", length = 1000)
    private String couponDescription;

    @Column(name = "win_probability", nullable = false, precision = 6, scale = 4)
    private BigDecimal winProbability;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "total_issued", nullable = false)
    @Builder.Default
    private Integer totalIssued = 0;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    public void touchUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }
}
