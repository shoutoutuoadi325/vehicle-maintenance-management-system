package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.GreenEnergyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GreenEnergyAccountRepository extends JpaRepository<GreenEnergyAccount, Long> {

    /**
     * 按用户ID查询绿色能量账户
     */
    Optional<GreenEnergyAccount> findByUserId(Long userId);

        List<GreenEnergyAccount> findTop20ByOrderByTotalEnergyDescIdAsc();

        List<GreenEnergyAccount> findTop20ByOrderByCurrentMileageDescIdAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GreenEnergyAccount a set a.totalEnergy = a.totalEnergy + :energyDelta, " +
            "a.currentMileage = a.currentMileage + :mileageDelta where a.userId = :userId")
    int atomicIncrease(@Param("userId") Long userId,
                       @Param("energyDelta") int energyDelta,
                       @Param("mileageDelta") int mileageDelta);
}
