ALTER TABLE maintenance_alert
    ADD COLUMN dedup_key VARCHAR(128) NULL;

UPDATE maintenance_alert
SET dedup_key = CONCAT(
    user_id,
    ':',
    vehicle_id,
    ':',
    alert_type,
    ':',
    DATE_FORMAT(trigger_time, '%Y%m%d')
)
WHERE dedup_key IS NULL;

DELETE m1
FROM maintenance_alert m1
JOIN maintenance_alert m2
    ON m1.dedup_key = m2.dedup_key
    AND m1.id > m2.id;

ALTER TABLE maintenance_alert
    MODIFY COLUMN dedup_key VARCHAR(128) NOT NULL;

CREATE UNIQUE INDEX uk_maintenance_alert_dedup_key ON maintenance_alert(dedup_key);ALTER TABLE maintenance_alert
    ADD COLUMN dedup_key VARCHAR(128) NULL;

UPDATE maintenance_alert
SET dedup_key = CONCAT(
    user_id,
    ':',
    vehicle_id,
    ':',
    alert_type,
    ':',
    DATE_FORMAT(trigger_time, '%Y%m%d')
)
WHERE dedup_key IS NULL;

DELETE m1
FROM maintenance_alert m1
JOIN maintenance_alert m2
    ON m1.dedup_key = m2.dedup_key
    AND m1.id > m2.id;

ALTER TABLE maintenance_alert
    MODIFY COLUMN dedup_key VARCHAR(128) NOT NULL;

CREATE UNIQUE INDEX uk_maintenance_alert_dedup_key ON maintenance_alert(dedup_key);
