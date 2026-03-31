ALTER TABLE vehicle
    ADD COLUMN current_mileage INT NULL,
    ADD COLUMN last_maintenance_mileage INT NULL,
    ADD COLUMN last_maintenance_at TIMESTAMP NULL;

ALTER TABLE material
    ADD COLUMN stock_quantity INT NOT NULL DEFAULT 0,
    ADD COLUMN minimum_stock_level INT NOT NULL DEFAULT 10;

CREATE TABLE maintenance_alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    alert_type VARCHAR(40) NOT NULL,
    message VARCHAR(500) NOT NULL,
    trigger_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_maintenance_alert_user_id ON maintenance_alert(user_id);
CREATE INDEX idx_maintenance_alert_vehicle_id ON maintenance_alert(vehicle_id);
CREATE INDEX idx_maintenance_alert_status ON maintenance_alert(status);

CREATE TABLE inventory_alert_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(120) NOT NULL,
    current_stock INT NOT NULL,
    minimum_stock_level INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL
);

CREATE INDEX idx_inventory_alert_material_id ON inventory_alert_notification(material_id);
CREATE INDEX idx_inventory_alert_status ON inventory_alert_notification(status);
