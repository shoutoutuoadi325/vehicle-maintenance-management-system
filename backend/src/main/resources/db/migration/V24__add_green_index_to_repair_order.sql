-- V24__add_green_index_to_repair_order.sql
ALTER TABLE repair_order ADD COLUMN green_index VARCHAR(16);
ALTER TABLE repair_order ADD COLUMN green_recommendation TEXT;
