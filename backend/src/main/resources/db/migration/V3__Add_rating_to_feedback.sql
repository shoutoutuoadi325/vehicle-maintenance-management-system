-- 为Feedback表添加rating字段
SET @rating_col_exists = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = DATABASE()
	  AND table_name = 'feedback'
	  AND column_name = 'rating'
);

SET @ddl = IF(
	@rating_col_exists = 0,
	'ALTER TABLE feedback ADD COLUMN rating INTEGER',
	'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加约束确保rating在1-5之间
SET @chk_rating_exists = (
	SELECT COUNT(*)
	FROM information_schema.table_constraints
	WHERE constraint_schema = DATABASE()
	  AND table_name = 'feedback'
	  AND constraint_name = 'chk_rating'
);

SET @ddl = IF(
	@chk_rating_exists = 0,
	'ALTER TABLE feedback ADD CONSTRAINT chk_rating CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5))',
	'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;