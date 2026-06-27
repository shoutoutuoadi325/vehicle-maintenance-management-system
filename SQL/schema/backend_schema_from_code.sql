SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `order_technician`;
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `repair_order`;
DROP TABLE IF EXISTS `vehicle`;
DROP TABLE IF EXISTS `technician`;
DROP TABLE IF EXISTS `material`;
DROP TABLE IF EXISTS `user_coupon_wallet`;
DROP TABLE IF EXISTS `coupon`;
DROP TABLE IF EXISTS `brand_partner`;
DROP TABLE IF EXISTS `inventory_alert_notification`;
DROP TABLE IF EXISTS `maintenance_alert`;
DROP TABLE IF EXISTS `journey_footprint`;
DROP TABLE IF EXISTS `journey_completion_record`;
DROP TABLE IF EXISTS `green_journey_node_state`;
DROP TABLE IF EXISTS `green_reward_ledger`;
DROP TABLE IF EXISTS `green_daily_quota`;
DROP TABLE IF EXISTS `green_energy_account`;
DROP TABLE IF EXISTS `green_rule_config`;
DROP TABLE IF EXISTS `green_quiz`;
DROP TABLE IF EXISTS `journey_map_node`;
DROP TABLE IF EXISTS `journey_map`;
DROP TABLE IF EXISTS `auth_refresh_token`;
DROP TABLE IF EXISTS `admin`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `admin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) DEFAULT NULL,
  `role` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `technician` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `employee_id` VARCHAR(255) NOT NULL,
  `username` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) DEFAULT NULL,
  `skill_type` VARCHAR(64) NOT NULL,
  `hourly_rate` DOUBLE NOT NULL,
  `total_work_hours` DOUBLE DEFAULT NULL,
  `completed_orders` INT DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `employee_id` (`employee_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `unit_price` DOUBLE NOT NULL,
  `stock_quantity` INT NOT NULL DEFAULT 0,
  `minimum_stock_level` INT NOT NULL DEFAULT 10,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `vehicle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `license_plate` VARCHAR(255) NOT NULL,
  `brand` VARCHAR(255) NOT NULL,
  `model` VARCHAR(255) NOT NULL,
  `year` INT DEFAULT NULL,
  `color` VARCHAR(255) DEFAULT NULL,
  `vin` VARCHAR(255) DEFAULT NULL,
  `current_mileage` INT DEFAULT NULL,
  `last_maintenance_mileage` INT DEFAULT NULL,
  `last_maintenance_at` DATETIME DEFAULT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_vehicle_user` (`user_id`),
  CONSTRAINT `fk_vehicle_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `repair_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_number` VARCHAR(255) NOT NULL,
  `status` VARCHAR(64) NOT NULL,
  `description` TEXT NOT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  `started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `repair_ended_at` DATETIME DEFAULT NULL,
  `labor_cost` DOUBLE DEFAULT NULL,
  `estimated_emission` DOUBLE DEFAULT NULL,
  `eco_material` TINYINT(1) DEFAULT NULL,
  `rework_count` INT DEFAULT NULL,
  `repair_type` VARCHAR(64) DEFAULT NULL,
  `material_cost` DOUBLE DEFAULT NULL,
  `total_cost` DOUBLE DEFAULT NULL,
  `estimated_hours` DOUBLE DEFAULT NULL,
  `actual_hours` DOUBLE DEFAULT NULL,
  `assignment_type` VARCHAR(64) DEFAULT NULL,
  `required_skill_type` VARCHAR(64) DEFAULT NULL,
  `urge_status` VARCHAR(64) DEFAULT NULL,
  `user_id` BIGINT NOT NULL,
  `vehicle_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_repair_order_user` (`user_id`),
  KEY `fk_repair_order_vehicle` (`vehicle_id`),
  CONSTRAINT `fk_repair_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_repair_order_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_technician` (
  `order_id` BIGINT NOT NULL,
  `technician_id` BIGINT NOT NULL,
  PRIMARY KEY (`order_id`, `technician_id`),
  KEY `idx_order_technician_technician` (`technician_id`),
  CONSTRAINT `fk_order_technician_order` FOREIGN KEY (`order_id`) REFERENCES `repair_order` (`id`),
  CONSTRAINT `fk_order_technician_technician` FOREIGN KEY (`technician_id`) REFERENCES `technician` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rating` INT DEFAULT NULL,
  `comment` TEXT,
  `created_at` DATETIME NOT NULL,
  `repair_order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_feedback_repair_order` (`repair_order_id`),
  KEY `fk_feedback_user` (`user_id`),
  CONSTRAINT `fk_feedback_repair_order` FOREIGN KEY (`repair_order_id`) REFERENCES `repair_order` (`id`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_rating` CHECK (`rating` IS NULL OR (`rating` >= 1 AND `rating` <= 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `brand_partner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `brand_name` VARCHAR(120) NOT NULL,
  `brand_code` VARCHAR(60) NOT NULL,
  `logo_url` VARCHAR(500) DEFAULT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_partner_code` (`brand_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `brand_partner_id` BIGINT NOT NULL,
  `city_index` INT NOT NULL,
  `coupon_title` VARCHAR(255) NOT NULL,
  `coupon_description` VARCHAR(1000) DEFAULT NULL,
  `win_probability` DECIMAL(6,4) NOT NULL DEFAULT 0.2000,
  `stock` INT DEFAULT NULL,
  `total_issued` INT NOT NULL DEFAULT 0,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `expire_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_city_enabled` (`city_index`, `enabled`),
  KEY `idx_coupon_brand` (`brand_partner_id`),
  CONSTRAINT `fk_coupon_brand_partner` FOREIGN KEY (`brand_partner_id`) REFERENCES `brand_partner` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_coupon_wallet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `coupon_id` BIGINT NOT NULL,
  `brand_partner_id` BIGINT NOT NULL,
  `city_index` INT NOT NULL,
  `coupon_title` VARCHAR(255) NOT NULL,
  `coupon_description` VARCHAR(1000) DEFAULT NULL,
  `coupon_status` VARCHAR(20) NOT NULL DEFAULT 'NEW',
  `source_action` VARCHAR(120) NOT NULL,
  `draw_time` DATETIME NOT NULL,
  `expire_time` DATETIME DEFAULT NULL,
  `redeem_time` DATETIME DEFAULT NULL,
  `redeem_shop_id` BIGINT DEFAULT NULL,
  `redeem_technician_id` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_wallet_user_status` (`user_id`, `coupon_status`),
  KEY `idx_wallet_city` (`city_index`),
  KEY `idx_wallet_redeem_shop` (`redeem_shop_id`),
  KEY `idx_wallet_redeem_technician` (`redeem_technician_id`),
  UNIQUE KEY `uk_wallet_source` (`source_action`),
  CONSTRAINT `fk_wallet_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_wallet_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`),
  CONSTRAINT `fk_wallet_brand_partner` FOREIGN KEY (`brand_partner_id`) REFERENCES `brand_partner` (`id`),
  CONSTRAINT `fk_wallet_redeem_shop` FOREIGN KEY (`redeem_shop_id`) REFERENCES `brand_partner` (`id`),
  CONSTRAINT `fk_wallet_redeem_technician` FOREIGN KEY (`redeem_technician_id`) REFERENCES `technician` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `journey_map` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `map_name` VARCHAR(120) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_journey_map_name` (`map_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `journey_map_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `map_id` BIGINT NOT NULL,
  `city_index` INT NOT NULL,
  `city_name` VARCHAR(120) NOT NULL,
  `required_mileage` INT NOT NULL,
  `x` INT NOT NULL,
  `y` INT NOT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_journey_map_node` (`map_id`, `city_index`),
  KEY `idx_journey_map_node_map` (`map_id`),
  CONSTRAINT `fk_journey_map_node_map` FOREIGN KEY (`map_id`) REFERENCES `journey_map` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `journey_completion_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `completed_at` DATETIME NOT NULL,
  `grand_prize_granted` TINYINT(1) NOT NULL DEFAULT 0,
  `sticker_claimed` TINYINT(1) NOT NULL DEFAULT 0,
  `consignee_name` VARCHAR(100) DEFAULT NULL,
  `consignee_phone` VARCHAR(30) DEFAULT NULL,
  `shipping_address` VARCHAR(500) DEFAULT NULL,
  `shipping_status` VARCHAR(30) NOT NULL DEFAULT 'NOT_CLAIMED',
  `shipment_tracking_no` VARCHAR(80) DEFAULT NULL,
  `shipped_at` DATETIME DEFAULT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_journey_completion_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `green_quiz` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `question` VARCHAR(1000) NOT NULL,
  `city_index` INT DEFAULT NULL,
  `event_title` VARCHAR(255) DEFAULT NULL,
  `event_description` VARCHAR(1000) DEFAULT NULL,
  `event_theme` VARCHAR(50) DEFAULT NULL,
  `is_default_for_city` TINYINT(1) NOT NULL DEFAULT 0,
  `options` TEXT NOT NULL,
  `correct_answer` VARCHAR(100) NOT NULL,
  `energy_reward` INT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `green_rule_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_key` VARCHAR(100) NOT NULL,
  `rule_value` VARCHAR(255) NOT NULL,
  `value_type` VARCHAR(20) NOT NULL DEFAULT 'STRING',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_green_rule_key` (`rule_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `green_energy_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `total_energy` INT NOT NULL DEFAULT 0,
  `current_mileage` INT NOT NULL DEFAULT 0,
  `current_map_id` BIGINT NOT NULL,
  `journey_status` VARCHAR(40) NOT NULL DEFAULT 'NORMAL',
  `pending_random_quiz_id` BIGINT DEFAULT NULL,
  `frozen_mileage` INT NOT NULL DEFAULT 0,
  `random_event_next_retry_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_green_energy_account_user` (`user_id`),
  CONSTRAINT `fk_green_energy_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_green_energy_current_map` FOREIGN KEY (`current_map_id`) REFERENCES `journey_map` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `green_daily_quota` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `quota_date` DATE NOT NULL,
  `used_energy` INT NOT NULL DEFAULT 0,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_green_daily_quota_user_date` (`user_id`, `quota_date`),
  KEY `idx_green_daily_quota_date` (`quota_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `green_reward_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `source_type` VARCHAR(40) NOT NULL,
  `source_id` VARCHAR(100) NOT NULL,
  `action_key` VARCHAR(60) NOT NULL,
  `energy_delta` INT NOT NULL,
  `mileage_delta` INT NOT NULL,
  `risk_level` VARCHAR(20) NOT NULL DEFAULT 'LOW',
  `reason` VARCHAR(255) NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_green_reward_source_action` (`source_type`, `source_id`, `action_key`),
  KEY `idx_green_reward_user_time` (`user_id`, `created_at`),
  CONSTRAINT `fk_green_reward_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `green_journey_node_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `map_id` BIGINT NOT NULL,
  `city_index` INT NOT NULL,
  `node_state` VARCHAR(20) NOT NULL,
  `checkin_at` DATETIME DEFAULT NULL,
  `next_retry_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_green_journey_user_map_city` (`user_id`, `map_id`, `city_index`),
  KEY `idx_green_journey_user` (`user_id`),
  KEY `idx_green_journey_user_map` (`user_id`, `map_id`),
  CONSTRAINT `fk_green_journey_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_green_journey_map` FOREIGN KEY (`map_id`) REFERENCES `journey_map` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `journey_footprint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `map_id` BIGINT NOT NULL,
  `event_type` VARCHAR(60) NOT NULL,
  `event_description` VARCHAR(500) NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_footprint_user_created` (`user_id`, `created_at` DESC),
  KEY `idx_footprint_map_created` (`map_id`, `created_at` DESC),
  CONSTRAINT `fk_footprint_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_footprint_map` FOREIGN KEY (`map_id`) REFERENCES `journey_map` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `auth_refresh_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `jti` VARCHAR(100) NOT NULL,
  `token_hash` VARCHAR(128) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `username` VARCHAR(100) NOT NULL,
  `user_role` VARCHAR(40) NOT NULL,
  `device_id` VARCHAR(120) DEFAULT NULL,
  `device_name` VARCHAR(120) DEFAULT NULL,
  `user_agent` VARCHAR(255) DEFAULT NULL,
  `ip_address` VARCHAR(64) DEFAULT NULL,
  `expires_at` DATETIME NOT NULL,
  `revoked` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `last_active_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_refresh_token_jti` (`jti`),
  KEY `idx_auth_refresh_user` (`user_id`, `user_role`),
  KEY `idx_auth_refresh_exp` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `maintenance_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `vehicle_id` BIGINT NOT NULL,
  `dedup_key` VARCHAR(128) NOT NULL,
  `alert_type` VARCHAR(40) NOT NULL,
  `message` VARCHAR(500) NOT NULL,
  `trigger_time` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_maintenance_alert_dedup_key` (`dedup_key`),
  KEY `idx_maintenance_alert_user_id` (`user_id`),
  KEY `idx_maintenance_alert_vehicle_id` (`vehicle_id`),
  KEY `idx_maintenance_alert_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `inventory_alert_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_id` BIGINT NOT NULL,
  `material_name` VARCHAR(120) NOT NULL,
  `current_stock` INT NOT NULL,
  `minimum_stock_level` INT NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL,
  `resolved_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inventory_alert_material_id` (`material_id`),
  KEY `idx_inventory_alert_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;