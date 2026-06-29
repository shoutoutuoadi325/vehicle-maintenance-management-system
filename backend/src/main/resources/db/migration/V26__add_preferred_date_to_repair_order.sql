ALTER TABLE repair_order
ADD COLUMN preferred_date DATETIME NULL AFTER created_at;
