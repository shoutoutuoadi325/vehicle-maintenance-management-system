package org.com.repair.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
public class MaintenanceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "vehicle_id", nullable = false, insertable = false, updatable = false)
    private Long vehicleId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date triggeredAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.UNREAD;

    @Column
    private String message;

    public enum AlertType {
        MILEAGE_OVERDUE,   // 里程超限
        TIME_OVERDUE       // 时间超限
    }

    public enum AlertStatus {
        UNREAD,
        READ
    }

    public MaintenanceAlert() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.vehicleId = vehicle != null ? vehicle.getId() : null;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public Date getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Date triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
