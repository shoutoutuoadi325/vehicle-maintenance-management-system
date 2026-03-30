package org.com.repair.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.com.repair.entity.MaintenanceAlert;
import org.com.repair.entity.MaintenanceAlert.AlertStatus;
import org.com.repair.entity.MaintenanceAlert.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceAlertRepository extends JpaRepository<MaintenanceAlert, Long> {

    List<MaintenanceAlert> findByUserIdOrderByTriggerTimeDesc(Long userId);

        Page<MaintenanceAlert> findByUserIdOrderByTriggerTimeDesc(Long userId, Pageable pageable);

        @Query("""
                        select a from MaintenanceAlert a
                        where a.userId = :userId
                            and (:status is null or a.status = :status)
                            and (:alertType is null or a.alertType = :alertType)
                        order by a.triggerTime desc
                        """)
        Page<MaintenanceAlert> searchByUserId(Long userId,
                                                                                    AlertStatus status,
                                                                                    AlertType alertType,
                                                                                    Pageable pageable);

    long countByUserIdAndStatus(Long userId, AlertStatus status);

    long countByUserIdAndAlertTypeAndStatusIn(Long userId, AlertType alertType, Collection<AlertStatus> status);

    boolean existsByVehicleIdAndAlertTypeAndStatusIn(Long vehicleId, AlertType alertType, Collection<AlertStatus> statuses);

    Optional<MaintenanceAlert> findByIdAndUserId(Long id, Long userId);

        @Modifying
        @Query("""
                        update MaintenanceAlert a
                        set a.status = :toStatus
                        where a.userId = :userId
                            and a.status = :fromStatus
                        """)
        int updateStatusByUserId(Long userId, AlertStatus fromStatus, AlertStatus toStatus);

        @Modifying
        @Query("""
                        update MaintenanceAlert a
                        set a.status = :toStatus
                        where a.userId = :userId
                            and a.id in :ids
                            and a.status = :fromStatus
                        """)
        int updateStatusByUserIdAndIds(Long userId,
                                                                     Collection<Long> ids,
                                                                     AlertStatus fromStatus,
                                                                     AlertStatus toStatus);
}
