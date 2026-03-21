package org.com.repair.repository;

import java.time.LocalDateTime;

import org.com.repair.entity.GreenRewardLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GreenRewardLedgerRepository extends JpaRepository<GreenRewardLedger, Long> {

    @Query("select coalesce(sum(r.energyDelta), 0) from GreenRewardLedger r where r.userId = :userId and r.createdAt >= :start and r.createdAt < :end and r.energyDelta > 0")
    Integer sumRewardedEnergyInWindow(@Param("userId") Long userId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("select count(r) from GreenRewardLedger r where r.userId = :userId and r.sourceType = :sourceType and r.createdAt >= :start and r.createdAt < :end")
    long countBySourceTypeInWindow(@Param("userId") Long userId,
                                   @Param("sourceType") String sourceType,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);
}
