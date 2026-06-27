/*
 Navicat Premium Dump SQL

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80408 (8.4.8)
 Source Host           : localhost:3306
 Source Schema         : car_repair

 Target Server Type    : MySQL
 Target Server Version : 80408 (8.4.8)
 File Encoding         : 65001

 Date: 28/03/2026 13:26:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of admin
-- ----------------------------
BEGIN;
INSERT INTO `admin` (`id`, `username`, `password`, `name`, `phone`, `email`, `role`) VALUES (1, 'admin', 'admin', '超级管理员', '13800138000', 'admin@example.com', 'SUPER_ADMIN');
INSERT INTO `admin` (`id`, `username`, `password`, `name`, `phone`, `email`, `role`) VALUES (2, 'manager1', 'password', '李经理', '13800138005', 'manager1@example.com', 'MANAGER');
COMMIT;

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rating` int DEFAULT NULL,
  `comment` text,
  `created_at` datetime NOT NULL,
  `repair_order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_feedback_repair_order` (`repair_order_id`),
  KEY `fk_feedback_user` (`user_id`),
  CONSTRAINT `fk_feedback_repair_order` FOREIGN KEY (`repair_order_id`) REFERENCES `repair_order` (`id`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of feedback
-- ----------------------------
BEGIN;
INSERT INTO `feedback` (`id`, `rating`, `comment`, `created_at`, `repair_order_id`, `user_id`) VALUES (1, 5, '服务态度非常好，维修质量也很满意', '2023-10-01 15:00:00', 1, 1);
INSERT INTO `feedback` (`id`, `rating`, `comment`, `created_at`, `repair_order_id`, `user_id`) VALUES (2, 4, '修理速度快，价格合理', '2023-10-02 10:00:00', 2, 2);
COMMIT;

-- ----------------------------
-- Table structure for material
-- ----------------------------
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `unit_price` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of material
-- ----------------------------
BEGIN;
INSERT INTO `material` (`id`, `name`, `unit_price`) VALUES (1, '钢材', 20);
INSERT INTO `material` (`id`, `name`, `unit_price`) VALUES (2, '橡胶', 15);
INSERT INTO `material` (`id`, `name`, `unit_price`) VALUES (3, '机油', 60);
INSERT INTO `material` (`id`, `name`, `unit_price`) VALUES (4, '车漆', 120);
COMMIT;

-- ----------------------------
-- Table structure for repair_order
-- ----------------------------
DROP TABLE IF EXISTS `repair_order`;
CREATE TABLE `repair_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_number` varchar(255) NOT NULL,
  `status` varchar(64) NOT NULL,
  `description` text NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `repair_ended_at` datetime DEFAULT NULL,
  `labor_cost` double DEFAULT NULL,
  `estimated_emission` double DEFAULT NULL,
  `eco_material` tinyint(1) DEFAULT NULL,
  `rework_count` int DEFAULT NULL,
  `repair_type` varchar(64) DEFAULT NULL,
  `material_cost` double DEFAULT NULL,
  `total_cost` double DEFAULT NULL,
  `estimated_hours` double DEFAULT NULL,
  `actual_hours` double DEFAULT NULL,
  `assignment_type` varchar(64) DEFAULT NULL,
  `required_skill_type` varchar(64) DEFAULT NULL,
  `urge_status` varchar(64) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `vehicle_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_repair_order_user` (`user_id`),
  KEY `fk_repair_order_vehicle` (`vehicle_id`),
  CONSTRAINT `fk_repair_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_repair_order_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of repair_order
-- ----------------------------
BEGIN;
INSERT INTO `repair_order` (`id`, `order_number`, `status`, `description`, `created_at`, `updated_at`, `started_at`, `completed_at`, `labor_cost`, `estimated_emission`, `eco_material`, `rework_count`, `repair_type`, `material_cost`, `total_cost`, `estimated_hours`, `actual_hours`, `assignment_type`, `required_skill_type`, `urge_status`, `user_id`, `vehicle_id`) VALUES (1, 'RO20231001', 'COMPLETED', '发动机启动困难，怠速不稳', '2023-10-01 10:00:00', '2023-10-01 12:00:00', NULL, '2023-10-01 14:00:00', 160, NULL, NULL, NULL, NULL, 200, 360, 2, 2, 'AUTO', 'MECHANIC', NULL, 1, 1);
INSERT INTO `repair_order` (`id`, `order_number`, `status`, `description`, `created_at`, `updated_at`, `started_at`, `completed_at`, `labor_cost`, `estimated_emission`, `eco_material`, `rework_count`, `repair_type`, `material_cost`, `total_cost`, `estimated_hours`, `actual_hours`, `assignment_type`, `required_skill_type`, `urge_status`, `user_id`, `vehicle_id`) VALUES (2, 'RO20231002', 'PENDING', '空调不制冷，可能是氟利昂不足', '2023-10-02 09:00:00', '2023-10-02 09:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3, NULL, 'AUTO', 'ELECTRICIAN', NULL, 2, 2);
COMMIT;

-- ----------------------------
-- Table structure for technician
-- ----------------------------
DROP TABLE IF EXISTS `technician`;
CREATE TABLE `technician` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `employee_id` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `skill_type` varchar(64) NOT NULL,
  `hourly_rate` double NOT NULL,
  `total_work_hours` double DEFAULT NULL,
  `completed_orders` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `employee_id` (`employee_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of technician
-- ----------------------------
BEGIN;
INSERT INTO `technician` (`id`, `name`, `employee_id`, `username`, `password`, `phone`, `email`, `skill_type`, `hourly_rate`, `total_work_hours`, `completed_orders`) VALUES (1, '王强', 'EMP001', 'tech1', 'password1', '13800138003', 'tech1@example.com', 'MECHANIC', 80, 0, 0);
INSERT INTO `technician` (`id`, `name`, `employee_id`, `username`, `password`, `phone`, `email`, `skill_type`, `hourly_rate`, `total_work_hours`, `completed_orders`) VALUES (2, '赵敏', 'EMP002', 'tech2', 'password2', '13800138004', 'tech2@example.com', 'ELECTRICIAN', 85, 0, 0);
COMMIT;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `email`, `address`) VALUES (1, 'user1', 'password1', '张伟', '13800138001', 'user1@example.com', '北京市朝阳区建国路1号');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `email`, `address`) VALUES (2, 'user2', 'password2', '李芳', '13800138002', 'user2@example.com', '上海市浦东新区人民路2号');
COMMIT;

-- ----------------------------
-- Table structure for vehicle
-- ----------------------------
DROP TABLE IF EXISTS `vehicle`;
CREATE TABLE `vehicle` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `license_plate` varchar(255) NOT NULL,
  `brand` varchar(255) NOT NULL,
  `model` varchar(255) NOT NULL,
  `year` int DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  `vin` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_vehicle_user` (`user_id`),
  CONSTRAINT `fk_vehicle_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of vehicle
-- ----------------------------
BEGIN;
INSERT INTO `vehicle` (`id`, `license_plate`, `brand`, `model`, `year`, `color`, `vin`, `user_id`) VALUES (1, '京A12345', '丰田', '卡罗拉', 2018, '白色', 'VIN123456789012345', 1);
INSERT INTO `vehicle` (`id`, `license_plate`, `brand`, `model`, `year`, `color`, `vin`, `user_id`) VALUES (2, '沪B67890', '本田', '思域', 2020, '黑色', 'VIN987654321098765', 2);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
