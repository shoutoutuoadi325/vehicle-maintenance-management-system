SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

/*
  方行唯“AI”智能汽车维修系统 - 本地样例数据

  预置账号：
  1. 管理员 admin / 123456
     覆盖总览指标、库存预警、最近订单、统计分析、调度看板。
  2. 客户 user / 123456
     覆盖车辆健康档案、保养提醒、维修进度、反馈、AI 问诊转工单、零碳旅程。
  3. 技师 tech / 123456
     覆盖个人任务、催单处理、收入统计、耗材消耗。

  数据设计思路：
  - 覆盖主要业务状态：待分配、已分配、进行中、已完成、已催单、低库存、负反馈。
  - 保持关联数据完整：避免空用户、空车辆、无技师的历史完成单。
  - 订单集中在最近 45 天，便于统计图表和看板刷新。
  - 前端可见的订单号使用 WX2026-001 这类业务编号。
*/

START TRANSACTION;

-- 0) 样例数据清理：兼容旧版 COMP2026-* 和新版 WX2026-*，不影响普通开发数据
DELETE f
FROM feedback f
JOIN repair_order ro ON ro.id = f.repair_order_id
WHERE ro.order_number LIKE 'COMP2026-%' OR ro.order_number LIKE 'WX2026-%';

DELETE ot
FROM order_technician ot
JOIN repair_order ro ON ro.id = ot.order_id
WHERE ro.order_number LIKE 'COMP2026-%' OR ro.order_number LIKE 'WX2026-%';

DELETE FROM repair_order WHERE order_number LIKE 'COMP2026-%' OR order_number LIKE 'WX2026-%';
DELETE FROM maintenance_alert WHERE dedup_key LIKE 'COMP:%';
DELETE FROM inventory_alert_notification WHERE material_name LIKE 'COMP-%';
DELETE FROM journey_footprint WHERE event_type LIKE 'COMP_%';
DELETE FROM green_reward_ledger WHERE source_id LIKE 'COMP_%' OR source_type LIKE 'COMP_%';
DELETE FROM user_coupon_wallet WHERE source_action LIKE 'COMP_%';

SET @seed_user_id = (SELECT id FROM user WHERE username = 'user' LIMIT 1);
DELETE FROM user_coupon_wallet
WHERE user_id = @seed_user_id
  AND source_action LIKE 'JOURNEY_COUPON_DRAW_%';
DELETE FROM journey_footprint
WHERE user_id = @seed_user_id
  AND event_type IN ('CHECKIN_CITY', 'TRIGGER_RANDOM_EVENT', 'DRAW_COUPON', 'REACH_DESTINATION');
DELETE FROM green_reward_ledger
WHERE user_id = @seed_user_id
  AND source_type = 'JOURNEY_CITY';
DELETE FROM journey_completion_record WHERE user_id = @seed_user_id;
DELETE FROM green_journey_node_state WHERE user_id = @seed_user_id;
DELETE FROM green_daily_quota WHERE user_id = @seed_user_id;
DELETE FROM green_energy_account WHERE user_id = @seed_user_id;

DELETE FROM vehicle WHERE license_plate LIKE 'COMP-%';
DELETE FROM material WHERE name LIKE 'COMP-%';
DELETE FROM coupon WHERE coupon_title LIKE 'COMP-%';

-- 1) 账号：预置三类用户入口
INSERT INTO admin (username, password, name, phone, email, role)
VALUES
  ('admin', '123456', '林岚（运营总监）', '13820260001', 'admin.system@example.com', 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  phone = VALUES(phone),
  email = VALUES(email),
  role = VALUES(role);

INSERT INTO user (username, password, name, phone, email, address)
VALUES
  ('user', '123456', '陈予安', '13820260002', 'chen.yuan@example.com', '上海市浦东新区张江汽车创新园 8 号'),
  ('vip', '123456', '李思源', '13820260003', 'li.siyuan@example.com', '上海市闵行区新能源车主中心'),
  ('fleet', '123456', '星河共享车队', '13820260004', 'fleet@example.com', '上海市嘉定区智能网联示范区')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  phone = VALUES(phone),
  email = VALUES(email),
  address = VALUES(address);

INSERT INTO technician (name, employee_id, username, password, phone, email, skill_type, hourly_rate, total_work_hours, completed_orders)
VALUES
  ('李浩', 'COMP-TECH-001', 'tech', '123456', '13920260001', 'lihao@example.com', 'MECHANIC', 128, 0, 0),
  ('王敏', 'COMP-TECH-002', 'electric', '123456', '13920260002', 'wangmin@example.com', 'ELECTRICIAN', 148, 0, 0),
  ('周凯', 'COMP-TECH-003', 'diag', '123456', '13920260003', 'zhoukai@example.com', 'DIAGNOSTIC', 168, 0, 0),
  ('赵宁', 'COMP-TECH-004', 'body', '123456', '13920260004', 'zhaoning@example.com', 'BODY_WORK', 138, 0, 0),
  ('许清', 'COMP-TECH-005', 'paint', '123456', '13920260005', 'xuqing@example.com', 'PAINT', 132, 0, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  password = VALUES(password),
  phone = VALUES(phone),
  email = VALUES(email),
  skill_type = VALUES(skill_type),
  hourly_rate = VALUES(hourly_rate);

SET @admin_id = (SELECT id FROM admin WHERE username = 'admin' LIMIT 1);
SET @u_chen = (SELECT id FROM user WHERE username = 'user' LIMIT 1);
SET @u_li = (SELECT id FROM user WHERE username = 'vip' LIMIT 1);
SET @u_fleet = (SELECT id FROM user WHERE username = 'fleet' LIMIT 1);
SET @t_mechanic = (SELECT id FROM technician WHERE username = 'tech' LIMIT 1);
SET @t_electric = (SELECT id FROM technician WHERE username = 'electric' LIMIT 1);
SET @t_diag = (SELECT id FROM technician WHERE username = 'diag' LIMIT 1);
SET @t_body = (SELECT id FROM technician WHERE username = 'body' LIMIT 1);
SET @t_paint = (SELECT id FROM technician WHERE username = 'paint' LIMIT 1);

-- 2) 车辆：品牌和里程保持差异，便于统计、保养提醒和客户视角查询
INSERT INTO vehicle (license_plate, brand, model, year, color, vin, current_mileage, last_maintenance_mileage, last_maintenance_at, user_id)
VALUES
  ('COMP-SH-AI01', '蔚来', 'ET5 Touring', 2024, '星灰', 'COMPVIN00000000001', 28680, 18600, DATE_SUB(NOW(), INTERVAL 235 DAY), @u_chen),
  ('COMP-SH-AI02', '比亚迪', '汉 EV', 2023, '玄空黑', 'COMPVIN00000000002', 16240, 13900, DATE_SUB(NOW(), INTERVAL 55 DAY), @u_chen),
  ('COMP-SH-VIP1', '小鹏', 'G9', 2024, '银翼白', 'COMPVIN00000000003', 21430, 17600, DATE_SUB(NOW(), INTERVAL 188 DAY), @u_li),
  ('COMP-FLEET-01', '特斯拉', 'Model 3', 2022, '珍珠白', 'COMPVIN00000000004', 47800, 40800, DATE_SUB(NOW(), INTERVAL 130 DAY), @u_fleet),
  ('COMP-FLEET-02', '广汽埃安', 'AION Y', 2023, '极光绿', 'COMPVIN00000000005', 36520, 33400, DATE_SUB(NOW(), INTERVAL 65 DAY), @u_fleet);

SET @v_nio = (SELECT id FROM vehicle WHERE license_plate = 'COMP-SH-AI01' ORDER BY id DESC LIMIT 1);
SET @v_byd = (SELECT id FROM vehicle WHERE license_plate = 'COMP-SH-AI02' ORDER BY id DESC LIMIT 1);
SET @v_xpeng = (SELECT id FROM vehicle WHERE license_plate = 'COMP-SH-VIP1' ORDER BY id DESC LIMIT 1);
SET @v_tesla = (SELECT id FROM vehicle WHERE license_plate = 'COMP-FLEET-01' ORDER BY id DESC LIMIT 1);
SET @v_aion = (SELECT id FROM vehicle WHERE license_plate = 'COMP-FLEET-02' ORDER BY id DESC LIMIT 1);

-- 3) 库存：低库存预警要明显，正常库存也要有对照
INSERT INTO material (name, unit_price, stock_quantity, minimum_stock_level)
VALUES
  ('COMP-低滚阻轮胎 235/45R19', 880, 18, 12),
  ('COMP-能量回收制动片', 420, 4, 20),
  ('COMP-高压电池冷却液', 168, 2, 15),
  ('COMP-毫米波雷达支架', 260, 0, 8),
  ('COMP-环保水性清漆', 198, 28, 10),
  ('COMP-空调滤芯 HEPA', 118, 36, 18);

SET @m_tire = (SELECT id FROM material WHERE name = 'COMP-低滚阻轮胎 235/45R19' ORDER BY id DESC LIMIT 1);
SET @m_brake = (SELECT id FROM material WHERE name = 'COMP-能量回收制动片' ORDER BY id DESC LIMIT 1);
SET @m_coolant = (SELECT id FROM material WHERE name = 'COMP-高压电池冷却液' ORDER BY id DESC LIMIT 1);
SET @m_radar = (SELECT id FROM material WHERE name = 'COMP-毫米波雷达支架' ORDER BY id DESC LIMIT 1);
SET @m_paint = (SELECT id FROM material WHERE name = 'COMP-环保水性清漆' ORDER BY id DESC LIMIT 1);

INSERT INTO inventory_alert_notification (material_id, material_name, current_stock, minimum_stock_level, status, created_at, resolved_at)
VALUES
  (@m_brake, 'COMP-能量回收制动片', 4, 20, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL),
  (@m_coolant, 'COMP-高压电池冷却液', 2, 15, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL),
  (@m_radar, 'COMP-毫米波雷达支架', 0, 8, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL);

-- 4) 维修订单：覆盖统计、客户进度、技师任务、调度重派、负反馈
INSERT INTO repair_order (
  order_number, status, description,
  created_at, updated_at, started_at, completed_at, repair_ended_at,
  labor_cost, estimated_emission, eco_material, rework_count, repair_type,
  material_cost, total_cost, estimated_hours, actual_hours,
  assignment_type, required_skill_type, urge_status,
  user_id, vehicle_id
) VALUES
  ('WX2026-001', 'COMPLETED', '三电系统年度健康体检：电池包压差、热管理水泵、绝缘状态全项检测',
   DATE_SUB(NOW(), INTERVAL 42 DAY), DATE_SUB(NOW(), INTERVAL 41 DAY), DATE_SUB(NOW(), INTERVAL 41 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY),
   592, 3.8, 1, 0, 'repair', 168, 760, 4.0, 4.0, 'AUTO', 'ELECTRICIAN', 'NOT_URGED', @u_chen, @v_nio),
  ('WX2026-002', 'COMPLETED', '高速制动抖动，前轮制动片与能量回收标定联检',
   DATE_SUB(NOW(), INTERVAL 36 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 34 DAY), DATE_SUB(NOW(), INTERVAL 34 DAY),
   512, 5.4, 0, 1, 'replace', 840, 1352, 4.0, 4.0, 'AUTO', 'MECHANIC', 'NOT_URGED', @u_li, @v_xpeng),
  ('WX2026-003', 'COMPLETED', '前保险杠轻微剐蹭，钣金整形与雷达支架校准',
   DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY),
   621, 2.9, 1, 0, 'repair', 260, 881, 4.5, 4.5, 'MANUAL', 'BODY_WORK', 'NOT_URGED', @u_fleet, @v_tesla),
  ('WX2026-004', 'COMPLETED', 'AION Y 空调异味与滤芯更换，顺带做低碳驾驶建议推送',
   DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY),
   252, 1.7, 1, 0, 'replace', 118, 370, 2.0, 2.0, 'AUTO', 'DIAGNOSTIC', 'NOT_URGED', @u_fleet, @v_aion),
  ('WX2026-005', 'COMPLETED', '车门划痕修复，使用环保水性漆并完成色差校验',
   DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY),
   528, 2.1, 1, 0, 'repair', 396, 924, 4.0, 4.0, 'AUTO', 'PAINT', 'NOT_URGED', @u_chen, @v_byd),
  ('WX2026-006', 'COMPLETED', '智能驾驶摄像头偶发离线，线束接插件排查与软件复位',
   DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY),
   504, 2.6, 1, 0, 'repair', 0, 504, 3.0, 3.0, 'AUTO', 'DIAGNOSTIC', 'NOT_URGED', @u_li, @v_xpeng),
  ('WX2026-007', 'IN_PROGRESS', '客户催单：刹车脚感偏软，能量回收切换时有顿挫，需要尽快交车',
   DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 7 HOUR), NULL, NULL,
   NULL, 6.8, 0, 1, 'repair', NULL, NULL, 5.0, NULL, 'AUTO', 'MECHANIC', 'URGED', @u_chen, @v_nio),
  ('WX2026-008', 'ASSIGNED', '底盘通过减速带异响，怀疑下摆臂胶套老化',
   DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NULL, NULL,
   NULL, 4.2, 1, 0, 'repair', NULL, NULL, 3.5, NULL, 'AUTO', 'MECHANIC', 'NOT_URGED', @u_fleet, @v_tesla),
  ('WX2026-009', 'ASSIGNED', '后保险杠泊车擦碰，需钣金复位后局部喷涂',
   DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NULL, NULL,
   NULL, 2.3, 1, 0, 'repair', NULL, NULL, 4.0, NULL, 'MANUAL', 'BODY_WORK', 'NOT_URGED', @u_li, @v_xpeng),
  ('WX2026-010', 'PENDING', 'AI 问诊转单：高压电池冷却液温度偏高，建议优先检测热管理回路',
   DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL, NULL, NULL,
   NULL, 7.5, 1, 0, 'repair', NULL, NULL, 3.0, NULL, 'AUTO', 'ELECTRICIAN', 'NOT_URGED', @u_chen, @v_nio),
  ('WX2026-011', 'PENDING', '毫米波雷达支架缺货：等待库存补齐后进行 ADAS 标定',
   DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NULL, NULL,
   NULL, 3.5, 0, 0, 'replace', NULL, NULL, 2.5, NULL, 'AUTO', 'DIAGNOSTIC', 'NOT_URGED', @u_fleet, @v_aion),
  ('WX2026-012', 'CANCELLED', '用户临时取消：轮胎换位预约改期',
   DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL,
   0, 0.8, 1, 0, 'repair', 0, 0, 1.0, 0, 'MANUAL', 'MECHANIC', 'NOT_URGED', @u_fleet, @v_aion);

SET @o001 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-001' LIMIT 1);
SET @o002 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-002' LIMIT 1);
SET @o003 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-003' LIMIT 1);
SET @o004 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-004' LIMIT 1);
SET @o005 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-005' LIMIT 1);
SET @o006 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-006' LIMIT 1);
SET @o007 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-007' LIMIT 1);
SET @o008 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-008' LIMIT 1);
SET @o009 = (SELECT id FROM repair_order WHERE order_number = 'WX2026-009' LIMIT 1);

INSERT INTO order_technician (order_id, technician_id)
VALUES
  (@o001, @t_electric),
  (@o002, @t_mechanic),
  (@o003, @t_body),
  (@o004, @t_diag),
  (@o005, @t_paint),
  (@o006, @t_diag),
  (@o007, @t_mechanic),
  (@o008, @t_mechanic),
  (@o009, @t_body);

-- 5) 反馈：既有五星样板，也有可被管理员追踪的负反馈
INSERT INTO feedback (rating, comment, created_at, repair_order_id, user_id)
VALUES
  (5, '检测报告非常清楚，技师解释了电池健康度和后续保养建议，服务闭环体验完整。', DATE_SUB(NOW(), INTERVAL 40 DAY), @o001, @u_chen),
  (2, '交车时间比承诺晚，且制动顿挫复现后沟通不够及时，建议主管复盘。', DATE_SUB(NOW(), INTERVAL 34 DAY), @o002, @u_li),
  (5, '钣金修复后雷达标定一次通过，车队批量维保效率很高。', DATE_SUB(NOW(), INTERVAL 28 DAY), @o003, @u_fleet),
  (4, '空调异味解决明显，低碳驾驶建议有帮助。', DATE_SUB(NOW(), INTERVAL 22 DAY), @o004, @u_fleet),
  (5, '水性漆色差控制很好，维修进度透明。', DATE_SUB(NOW(), INTERVAL 16 DAY), @o005, @u_chen);

-- 6) 保养提醒：客户首页能看到风险分层和批量已读
INSERT INTO maintenance_alert (user_id, vehicle_id, dedup_key, alert_type, message, trigger_time, status, created_at, updated_at)
VALUES
  (@u_chen, @v_nio, CONCAT('COMP:', @u_chen, ':', @v_nio, ':MILEAGE_OVERDUE'), 'MILEAGE_OVERDUE',
   '蔚来 ET5 距上次保养已超过 10000 km，建议优先检查制动与热管理系统。', DATE_SUB(NOW(), INTERVAL 2 DAY), 'UNREAD', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (@u_chen, @v_nio, CONCAT('COMP:', @u_chen, ':', @v_nio, ':TIME_OVERDUE'), 'TIME_OVERDUE',
   '距离上次保养已超过 6 个月，建议预约三电系统健康检查。', DATE_SUB(NOW(), INTERVAL 1 DAY), 'UNREAD', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (@u_chen, @v_byd, CONCAT('COMP:', @u_chen, ':', @v_byd, ':MILEAGE_NOTICE'), 'MILEAGE_OVERDUE',
   '比亚迪汉 EV 即将到达下一保养里程，可提前预约避免排队。', DATE_SUB(NOW(), INTERVAL 6 HOUR), 'READ', DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- 7) 零碳旅程：预置接近完成的路线状态，便于验证完整权益闭环
INSERT INTO journey_map (map_name, enabled, update_time)
VALUES ('COMP-318 川藏零碳服务线', 1, NOW())
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), update_time = VALUES(update_time);

SET @map_id = (SELECT MIN(id) FROM journey_map WHERE map_name = 'COMP-318 川藏零碳服务线');

-- 清理早期错误导入留下的 COMP-318 乱码路线，避免零碳之路路线列表显示 ??????。
DROP TEMPORARY TABLE IF EXISTS tmp_bad_comp_maps;
CREATE TEMPORARY TABLE tmp_bad_comp_maps (id BIGINT PRIMARY KEY);
INSERT INTO tmp_bad_comp_maps (id)
SELECT id
FROM journey_map
WHERE map_name LIKE 'COMP-318%'
  AND map_name LIKE BINARY '%?%';

UPDATE green_energy_account
SET current_map_id = @map_id
WHERE current_map_id IN (SELECT id FROM tmp_bad_comp_maps);

DELETE FROM journey_footprint
WHERE map_id IN (SELECT id FROM tmp_bad_comp_maps);

DELETE FROM green_journey_node_state
WHERE map_id IN (SELECT id FROM tmp_bad_comp_maps);

DELETE FROM journey_map_node
WHERE map_id IN (SELECT id FROM tmp_bad_comp_maps);

DELETE FROM journey_map
WHERE id IN (SELECT id FROM tmp_bad_comp_maps);

DROP TEMPORARY TABLE IF EXISTS tmp_bad_comp_maps;

-- 禁用重复路线副本，只保留一个正确的路线入口。
UPDATE journey_map
SET enabled = 0, update_time = NOW()
WHERE map_name LIKE 'COMP-318%'
  AND id <> @map_id;

UPDATE journey_map
SET enabled = 1, update_time = NOW()
WHERE id = @map_id;

INSERT INTO journey_map_node (map_id, city_index, city_name, required_mileage, x, y, update_time)
VALUES
  (@map_id, 0, '上海启程', 0, 70, 470, NOW()),
  (@map_id, 1, '湖州服务区', 110, 275, 355, NOW()),
  (@map_id, 2, '武汉充电港', 260, 470, 270, NOW()),
  (@map_id, 3, '成都低碳站', 430, 680, 195, NOW()),
  (@map_id, 4, '拉萨终点礼盒', 620, 940, 95, NOW())
ON DUPLICATE KEY UPDATE
  city_name = VALUES(city_name),
  required_mileage = VALUES(required_mileage),
  x = VALUES(x),
  y = VALUES(y),
  update_time = VALUES(update_time);

INSERT INTO green_energy_account (
  user_id, total_energy, current_mileage, current_map_id, journey_status,
  pending_random_quiz_id, frozen_mileage, random_event_next_retry_time,
  update_time, version
) VALUES (
  @u_chen, 405, 405, @map_id, 'NORMAL',
  NULL, 0, NULL,
  NOW(), 0
);

INSERT INTO green_daily_quota (user_id, quota_date, used_energy, update_time)
VALUES (@u_chen, CURDATE(), 260, NOW())
ON DUPLICATE KEY UPDATE used_energy = VALUES(used_energy), update_time = VALUES(update_time);

INSERT INTO green_journey_node_state (user_id, map_id, city_index, node_state, checkin_at, next_retry_time, update_time, version)
SELECT
  @u_chen,
  @map_id,
  n.city_index,
  CASE
    WHEN n.city_index <= 1 THEN 'CHECKED_IN'
    WHEN n.city_index = 2 THEN 'UNLOCKED'
    ELSE 'LOCKED'
  END,
  CASE
    WHEN n.city_index <= 1 THEN DATE_SUB(NOW(), INTERVAL (8 - n.city_index) DAY)
    ELSE NULL
  END,
  NULL,
  NOW(),
  0
FROM journey_map_node n
WHERE n.map_id = @map_id
ORDER BY n.city_index;

INSERT INTO green_reward_ledger (user_id, source_type, source_id, action_key, energy_delta, mileage_delta, risk_level, reason, created_at)
VALUES
  (@u_chen, 'REPAIR_ORDER', 'COMP_ORDER_001', 'EMISSION_REWARD', 180, 180, 'LOW', '环保材料维修奖励：三电健康体检', DATE_SUB(NOW(), INTERVAL 40 DAY)),
  (@u_chen, 'REPAIR_ORDER', 'COMP_ORDER_005', 'EMISSION_REWARD', 130, 130, 'LOW', '环保水性漆维修奖励', DATE_SUB(NOW(), INTERVAL 16 DAY)),
  (@u_chen, 'JOURNEY_CITY', 'COMP_CITY_0', 'CITY_CHECKIN_REWARD', 20, 20, 'LOW', '上海启程打卡', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@u_chen, 'JOURNEY_CITY', 'COMP_CITY_1', 'CITY_CHECKIN_REWARD', 25, 25, 'LOW', '湖州服务区打卡', DATE_SUB(NOW(), INTERVAL 7 DAY));

INSERT INTO journey_footprint (user_id, map_id, event_type, event_description, created_at)
VALUES
  (@u_chen, @map_id, 'COMP_CHECKIN_CITY', '上海启程：完成低碳驾驶知识问答', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@u_chen, @map_id, 'COMP_CHECKIN_CITY', '湖州服务区：领取合作品牌补能券', DATE_SUB(NOW(), INTERVAL 7 DAY));

-- 8) 品牌权益：钱包里既有未使用券，也有已核销券，便于讲商业闭环
-- 当前库的 brand_partner.brand_code 未必有唯一约束，所以不用 ON DUPLICATE KEY，按编码显式修复历史乱码行。
UPDATE brand_partner
SET brand_name = '壳牌智行服务区',
    logo_url = '/assets/brands/shell.svg',
    description = '低碳机油与补能合作品牌',
    enabled = 1,
    update_time = NOW()
WHERE brand_code = 'COMP_SHELL';

INSERT INTO brand_partner (brand_name, brand_code, logo_url, description, enabled, update_time)
SELECT '壳牌智行服务区', 'COMP_SHELL', '/assets/brands/shell.svg', '低碳机油与补能合作品牌', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM brand_partner WHERE brand_code = 'COMP_SHELL');

UPDATE brand_partner
SET brand_name = '博世智能检测中心',
    logo_url = '/assets/brands/bosch.svg',
    description = '智能诊断与 ADAS 标定合作品牌',
    enabled = 1,
    update_time = NOW()
WHERE brand_code = 'COMP_BOSCH';

INSERT INTO brand_partner (brand_name, brand_code, logo_url, description, enabled, update_time)
SELECT '博世智能检测中心', 'COMP_BOSCH', '/assets/brands/bosch.svg', '智能诊断与 ADAS 标定合作品牌', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM brand_partner WHERE brand_code = 'COMP_BOSCH');

SET @bp_shell = (SELECT MIN(id) FROM brand_partner WHERE brand_code = 'COMP_SHELL');
SET @bp_bosch = (SELECT MIN(id) FROM brand_partner WHERE brand_code = 'COMP_BOSCH');

INSERT INTO coupon (brand_partner_id, city_index, coupon_title, coupon_description, win_probability, stock, total_issued, enabled, expire_time, update_time)
VALUES
  (@bp_shell, 1, 'COMP-壳牌低碳养护券', '到店可抵扣低碳机油与空调滤芯养护套餐 80 元', 0.3500, 300, 18, 1, DATE_ADD(NOW(), INTERVAL 90 DAY), NOW()),
  (@bp_bosch, 2, 'COMP-博世智能检测券', '到店可免费进行一次 ADAS 传感器快速检测', 1.0000, 200, 26, 1, DATE_ADD(NOW(), INTERVAL 120 DAY), NOW());

SET @coupon_shell = (SELECT id FROM coupon WHERE coupon_title = 'COMP-壳牌低碳养护券' ORDER BY id DESC LIMIT 1);
SET @coupon_bosch = (SELECT id FROM coupon WHERE coupon_title = 'COMP-博世智能检测券' ORDER BY id DESC LIMIT 1);

INSERT INTO user_coupon_wallet (
  user_id, coupon_id, brand_partner_id, city_index, coupon_title, coupon_description,
  coupon_status, source_action, draw_time, expire_time, redeem_time,
  redeem_shop_id, redeem_technician_id, update_time, version
) SELECT
  @u_chen, c.id, c.brand_partner_id, c.city_index, c.coupon_title, c.coupon_description,
  'NEW', 'COMP_WALLET_NEW_SHELL', DATE_SUB(NOW(), INTERVAL 7 DAY), c.expire_time, NULL,
  NULL, NULL, NOW(), 0
FROM coupon c
WHERE c.id = @coupon_shell;

-- 9) 让技师聚合数据与订单一致，收入统计更稳
UPDATE technician t
SET
  t.total_work_hours = (
    SELECT COALESCE(SUM(COALESCE(ro.actual_hours, 0)), 0)
    FROM order_technician ot
    JOIN repair_order ro ON ro.id = ot.order_id
    WHERE ot.technician_id = t.id
  ),
  t.completed_orders = (
    SELECT COUNT(*)
    FROM order_technician ot
    JOIN repair_order ro ON ro.id = ot.order_id
    WHERE ot.technician_id = t.id AND ro.status = 'COMPLETED'
  )
WHERE t.id IN (@t_mechanic, @t_electric, @t_diag, @t_body, @t_paint);

COMMIT;

-- 10) 导入后快速核对
SELECT 'login-admin' AS item, username, password FROM admin WHERE username = 'admin'
UNION ALL
SELECT 'login-customer', username, password FROM user WHERE username = 'user'
UNION ALL
SELECT 'login-technician', username, password FROM technician WHERE username = 'tech';

SELECT 'sample_orders' AS metric, COUNT(*) AS value FROM repair_order WHERE order_number LIKE 'WX2026-%'
UNION ALL
SELECT 'active_inventory_alerts', COUNT(*) FROM inventory_alert_notification WHERE material_name LIKE 'COMP-%' AND status = 'ACTIVE'
UNION ALL
SELECT 'customer_maintenance_alerts', COUNT(*) FROM maintenance_alert WHERE dedup_key LIKE 'COMP:%'
UNION ALL
SELECT 'feedback_rows', COUNT(*)
FROM feedback f JOIN repair_order ro ON ro.id = f.repair_order_id
WHERE ro.order_number LIKE 'WX2026-%'
UNION ALL
SELECT 'wallet_rows', COUNT(*) FROM user_coupon_wallet WHERE source_action LIKE 'COMP_%';
