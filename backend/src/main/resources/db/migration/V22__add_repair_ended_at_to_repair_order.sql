SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'repair_order'
      AND COLUMN_NAME = 'repair_ended_at'
);

SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE repair_order ADD COLUMN repair_ended_at DATETIME NULL',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE repair_order
SET repair_ended_at = completed_at
WHERE repair_ended_at IS NULL
    AND completed_at IS NOT NULL;
