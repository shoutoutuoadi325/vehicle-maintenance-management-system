package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.UserCouponWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCouponWalletRepository extends JpaRepository<UserCouponWallet, Long> {

    List<UserCouponWallet> findByUserIdOrderByDrawTimeDesc(Long userId);

    Optional<UserCouponWallet> findByIdAndUserId(Long id, Long userId);
}
