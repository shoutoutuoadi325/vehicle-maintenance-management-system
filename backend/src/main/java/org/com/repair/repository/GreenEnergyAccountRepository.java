package org.com.repair.repository;

import java.util.Optional;

import org.com.repair.entity.GreenEnergyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GreenEnergyAccountRepository extends JpaRepository<GreenEnergyAccount, Long> {

    /**
     * 按用户ID查询绿色能量账户
     */
    Optional<GreenEnergyAccount> findByUserId(Long userId);
}
