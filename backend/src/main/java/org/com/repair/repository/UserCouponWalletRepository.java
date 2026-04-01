package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.UserCouponWallet;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface UserCouponWalletRepository extends JpaRepository<UserCouponWallet, Long> {

    List<UserCouponWallet> findByUserIdOrderByDrawTimeDesc(Long userId);

    Optional<UserCouponWallet> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from UserCouponWallet w where w.id = :walletId")
    Optional<UserCouponWallet> findByIdForUpdate(@Param("walletId") Long walletId);
}
