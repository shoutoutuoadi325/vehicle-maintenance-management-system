SET @wallet_version_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_coupon_wallet'
      AND column_name = 'version'
);

SET @ddl = IF(
    @wallet_version_exists = 0,
    'ALTER TABLE user_coupon_wallet ADD COLUMN version BIGINT NOT NULL DEFAULT 0',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
