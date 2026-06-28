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
DELETE FROM green_quiz WHERE event_title LIKE 'COMP-%';

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

-- 7.1) 零碳旅程题库：和 COMP-318 演示路线强绑定，保证城市问答与随机事件都有样例
UPDATE green_quiz
SET
  event_title = 'COMP-上海启程低碳检查',
  event_description = '出发前完成车辆轻量化与胎压检查，开启本次零碳服务线。',
  event_theme = 'city',
  question = '从上海城区启程前，哪项准备最能降低长途能耗？',
  options = '{"A":"后备箱长期堆满重物","B":"清理多余载物并检查胎压","C":"出发前长时间原地怠速","D":"故意降低胎压提升舒适性"}',
  correct_answer = 'B',
  energy_reward = 20
WHERE city_index = 0 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 0, 1, 'COMP-上海启程低碳检查', '出发前完成车辆轻量化与胎压检查，开启本次零碳服务线。', 'city',
       '从上海城区启程前，哪项准备最能降低长途能耗？',
       '{"A":"后备箱长期堆满重物","B":"清理多余载物并检查胎压","C":"出发前长时间原地怠速","D":"故意降低胎压提升舒适性"}',
       'B', 20
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 0 AND is_default_for_city = 1);

UPDATE green_quiz
SET
  event_title = 'COMP-湖州服务区补能选择',
  event_description = '抵达湖州服务区，选择更合理的补能与休整策略。',
  event_theme = 'charging',
  question = '在服务区短暂停留时，新能源车辆更推荐哪种补能方式？',
  options = '{"A":"电量很低才临时急充到满","B":"结合路线在合理区间补能并同步休息","C":"关闭热管理系统继续高速行驶","D":"为省时间跳过所有检查"}',
  correct_answer = 'B',
  energy_reward = 22
WHERE city_index = 1 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 1, 1, 'COMP-湖州服务区补能选择', '抵达湖州服务区，选择更合理的补能与休整策略。', 'charging',
       '在服务区短暂停留时，新能源车辆更推荐哪种补能方式？',
       '{"A":"电量很低才临时急充到满","B":"结合路线在合理区间补能并同步休息","C":"关闭热管理系统继续高速行驶","D":"为省时间跳过所有检查"}',
       'B', 22
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 1 AND is_default_for_city = 1);

UPDATE green_quiz
SET
  event_title = 'COMP-武汉充电港热管理',
  event_description = '长江中游气温较高，考察电池热管理与空调能耗控制。',
  event_theme = 'battery',
  question = '高温天气长途行驶时，哪种做法更有利于电池效率与安全？',
  options = '{"A":"忽视电池温控提示","B":"保持热管理正常工作并避免连续激烈驾驶","C":"长期满载急加速","D":"关闭所有冷却功能省电"}',
  correct_answer = 'B',
  energy_reward = 22
WHERE city_index = 2 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 2, 1, 'COMP-武汉充电港热管理', '长江中游气温较高，考察电池热管理与空调能耗控制。', 'battery',
       '高温天气长途行驶时，哪种做法更有利于电池效率与安全？',
       '{"A":"忽视电池温控提示","B":"保持热管理正常工作并避免连续激烈驾驶","C":"长期满载急加速","D":"关闭所有冷却功能省电"}',
       'B', 22
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 2 AND is_default_for_city = 1);

UPDATE green_quiz
SET
  event_title = 'COMP-成都低碳站山路策略',
  event_description = '进入西部山地路段前，完成制动、胎压与能量回收策略判断。',
  event_theme = 'mountain',
  question = '进入连续坡道前，哪项操作更符合安全低碳驾驶？',
  options = '{"A":"下坡全程空挡滑行","B":"提前检查制动并合理使用能量回收","C":"频繁急刹测试制动","D":"关闭仪表告警避免干扰"}',
  correct_answer = 'B',
  energy_reward = 24
WHERE city_index = 3 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 3, 1, 'COMP-成都低碳站山路策略', '进入西部山地路段前，完成制动、胎压与能量回收策略判断。', 'mountain',
       '进入连续坡道前，哪项操作更符合安全低碳驾驶？',
       '{"A":"下坡全程空挡滑行","B":"提前检查制动并合理使用能量回收","C":"频繁急刹测试制动","D":"关闭仪表告警避免干扰"}',
       'B', 24
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 3 AND is_default_for_city = 1);

UPDATE green_quiz
SET
  event_title = 'COMP-拉萨终点绿色保养',
  event_description = '终点前完成最后一道绿色养护题，衔接大奖与权益闭环。',
  event_theme = 'final',
  question = '长途旅程结束后，哪项养护动作最符合绿色维修理念？',
  options = '{"A":"忽略异常噪声继续用车","B":"按周期检查轮胎、制动和滤芯状态","C":"等故障扩大后一次性维修","D":"只清洗外观不检查关键系统"}',
  correct_answer = 'B',
  energy_reward = 28
WHERE city_index = 4 AND is_default_for_city = 1;

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
SELECT 4, 1, 'COMP-拉萨终点绿色保养', '终点前完成最后一道绿色养护题，衔接大奖与权益闭环。', 'final',
       '长途旅程结束后，哪项养护动作最符合绿色维修理念？',
       '{"A":"忽略异常噪声继续用车","B":"按周期检查轮胎、制动和滤芯状态","C":"等故障扩大后一次性维修","D":"只清洗外观不检查关键系统"}',
       'B', 28
WHERE NOT EXISTS (SELECT 1 FROM green_quiz WHERE city_index = 4 AND is_default_for_city = 1);

INSERT INTO green_quiz (city_index, is_default_for_city, event_title, event_description, event_theme, question, options, correct_answer, energy_reward)
VALUES
  (0, 0, 'COMP-上海早高峰能耗挑战', '城市早高峰车流密集，选择更稳的起步和跟车方式。', 'traffic',
   '拥堵路段走走停停时，哪种驾驶方式更节能？',
   '{"A":"频繁地板油抢位","B":"保持车距并平顺加减速","C":"长时间原地轰油门","D":"急刹后立刻急加速"}',
   'B', 18),
  (1, 0, 'COMP-湖州胎压巡检', '服务区休整时发现胎压偏低，判断正确处理方式。', 'tire',
   '胎压长期偏低通常会带来什么影响？',
   '{"A":"滚阻增大、能耗上升","B":"一定降低能耗","C":"只影响车漆颜色","D":"完全不影响轮胎寿命"}',
   'A', 18),
  (2, 0, 'COMP-武汉雨天制动回收', '雨天经过武汉城区，考察制动能量回收与安全距离。', 'rain',
   '湿滑道路上使用能量回收时，更合理的做法是？',
   '{"A":"突然切到最高回收并紧跟前车","B":"结合路况平顺减速并保持安全距离","C":"关闭车灯节省电量","D":"高速压过积水区"}',
   'B', 20),
  (3, 0, 'COMP-成都拥堵绕行', '进入成都低碳站前遇到拥堵，选择路线规划策略。', 'route',
   '合理避开严重拥堵通常会带来什么效果？',
   '{"A":"减少怠速与频繁启停","B":"必然增加碳排放","C":"完全不影响能耗","D":"一定增加维修成本"}',
   'A', 20),
  (4, 0, 'COMP-拉萨高原温差', '终点附近昼夜温差大，关注胎压和热管理。', 'cold',
   '昼夜温差较大时，哪种用车习惯更稳妥？',
   '{"A":"冷车立即高负载行驶","B":"关注胎压变化并平稳起步","C":"故意超载提高续航","D":"忽略热管理告警"}',
   'B', 22),
  (NULL, 0, 'COMP-路途突发：空调能耗', '夏季服务区外温较高，选择更低碳的空调使用方式。', 'random',
   '高温行车时，哪种空调使用方式更兼顾舒适与节能？',
   '{"A":"长期开窗高速行驶","B":"先通风再合理设定空调温度","C":"空调温度越低越省电","D":"停车长时间怠速开空调"}',
   'B', 15),
  (NULL, 0, 'COMP-路途突发：制动异响', '长下坡后出现轻微制动异响，需要判断处理方式。', 'random',
   '发现制动异响后，哪种处理更安全也更绿色？',
   '{"A":"继续高速行驶直到完全失效","B":"降低车速并尽快到店检查制动片磨损","C":"用急刹反复磨合","D":"关闭音乐以免听到异响"}',
   'B', 15);

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
