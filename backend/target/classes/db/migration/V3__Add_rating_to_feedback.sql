-- 为Feedback表添加rating字段
ALTER TABLE feedback ADD COLUMN rating INTEGER;

-- 添加约束确保rating在1-5之间
ALTER TABLE feedback ADD CONSTRAINT chk_rating CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5)); 