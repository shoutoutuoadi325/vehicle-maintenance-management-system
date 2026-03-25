package org.com.repair.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.com.repair.entity.GreenDailyQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface GreenDailyQuotaRepository extends JpaRepository<GreenDailyQuota, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select q from GreenDailyQuota q where q.userId = :userId and q.quotaDate = :quotaDate")
        Optional<GreenDailyQuota> findByUserIdAndQuotaDateForUpdate(@Param("userId") Long userId,
                                                                                                                                 @Param("quotaDate") LocalDate quotaDate);

    @Modifying
    @Query(value = "INSERT IGNORE INTO green_daily_quota(user_id, quota_date, used_energy, update_time) VALUES (:userId, :quotaDate, 0, NOW())", nativeQuery = true)
    int initQuotaRow(@Param("userId") Long userId, @Param("quotaDate") LocalDate quotaDate);

    @Modifying
    @Query(value = "UPDATE green_daily_quota SET used_energy = used_energy + :delta, update_time = NOW() " +
            "WHERE user_id = :userId AND quota_date = :quotaDate AND used_energy + :delta <= :dailyCap", nativeQuery = true)
    int tryConsumeQuota(@Param("userId") Long userId,
                        @Param("quotaDate") LocalDate quotaDate,
                        @Param("delta") int delta,
                        @Param("dailyCap") int dailyCap);
}
