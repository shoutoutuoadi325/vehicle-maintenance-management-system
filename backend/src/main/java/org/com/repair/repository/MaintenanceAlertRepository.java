package org.com.repair.repository;

import org.com.repair.entity.MaintenanceAlert;
import org.com.repair.entity.MaintenanceAlert.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceAlertRepository extends JpaRepository<MaintenanceAlert, Long> {

    List<MaintenanceAlert> findByVehicleId(Long vehicleId);

    List<MaintenanceAlert> findByVehicleIdAndStatus(Long vehicleId, AlertStatus status);

    boolean existsByVehicleIdAndAlertTypeAndStatus(Long vehicleId,
            MaintenanceAlert.AlertType alertType, AlertStatus status);

    List<MaintenanceAlert> findByStatus(AlertStatus status);
}
