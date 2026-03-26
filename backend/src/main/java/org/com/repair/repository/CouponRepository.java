package org.com.repair.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.repair.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("select c from Coupon c where c.cityIndex = :cityIndex and c.enabled = true and (c.expireTime is null or c.expireTime > :now) and (c.stock is null or c.totalIssued < c.stock)")
    List<Coupon> findActiveByCityIndex(@Param("cityIndex") Integer cityIndex, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update Coupon c set c.totalIssued = c.totalIssued + 1 where c.id = :couponId and c.enabled = true and (c.stock is null or c.totalIssued < c.stock)")
    int tryIssueCoupon(@Param("couponId") Long couponId);

    Optional<Coupon> findTopByCityIndexAndEnabledTrueOrderByIdAsc(Integer cityIndex);
}
