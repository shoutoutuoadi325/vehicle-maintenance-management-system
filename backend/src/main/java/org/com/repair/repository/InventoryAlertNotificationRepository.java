package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.InventoryAlertNotification;
import org.com.repair.entity.InventoryAlertNotification.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryAlertNotificationRepository extends JpaRepository<InventoryAlertNotification, Long> {

    boolean existsByMaterialIdAndStatus(Long materialId, AlertStatus status);

    List<InventoryAlertNotification> findByStatusOrderByCreatedAtDesc(AlertStatus status);

        Page<InventoryAlertNotification> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);

        @Query("""
                        select n from InventoryAlertNotification n
                        where n.status = :status
                            and (
                                :severity is null
                                or (:severity = 'CRITICAL' and (n.currentStock <= 0 or n.currentStock * 2 <= n.minimumStockLevel))
                                or (:severity = 'WARNING' and (n.currentStock > 0 and n.currentStock * 2 > n.minimumStockLevel))
                            )
                        order by n.createdAt desc
                        """)
        Page<InventoryAlertNotification> searchByStatusAndSeverity(AlertStatus status,
                                                                                                                             String severity,
                                                                                                                             Pageable pageable);

        @Query("""
                        select count(n) from InventoryAlertNotification n
                        where n.status = :status
                            and (n.currentStock <= 0 or n.currentStock * 2 <= n.minimumStockLevel)
                        """)
        long countCriticalByStatus(AlertStatus status);

        @Query("""
                        select count(n) from InventoryAlertNotification n
                        where n.status = :status
                            and n.currentStock > 0
                            and n.currentStock * 2 > n.minimumStockLevel
                        """)
        long countWarningByStatus(AlertStatus status);

    Optional<InventoryAlertNotification> findByIdAndStatus(Long id, AlertStatus status);

    @Modifying
    @Query("""
            update InventoryAlertNotification n
            set n.status = :toStatus,
                n.resolvedAt = CURRENT_TIMESTAMP
            where n.id in :ids
              and n.status = :fromStatus
            """)
    int updateStatusByIds(List<Long> ids, AlertStatus fromStatus, AlertStatus toStatus);
}
