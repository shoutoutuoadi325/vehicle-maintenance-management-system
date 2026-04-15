SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;
START TRANSACTION;

-- 1) Upsert demo login accounts (three roles)
INSERT INTO admin (username, password, name, phone, email, role)
VALUES ('admin_demo', 'Pass@1234', 'Admin Demo', '13800000001', 'admin.demo@example.com', 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  phone = VALUES(phone),
  email = VALUES(email),
  role = VALUES(role);

INSERT INTO user (username, password, name, phone, email, address)
VALUES ('customer_demo', 'Pass@1234', 'Customer Demo', '13800000002', 'customer.demo@example.com', 'Demo Road 1')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  phone = VALUES(phone),
  email = VALUES(email),
  address = VALUES(address);

INSERT INTO technician (name, employee_id, username, password, phone, email, skill_type, hourly_rate, total_work_hours, completed_orders)
VALUES
  ('Technician Demo', 'EMP-DEMO-001', 'tech_demo', 'Pass@1234', '13800000003', 'tech.demo@example.com', 'MECHANIC', 120, 0, 0),
  ('Bodywork Demo', 'EMP-DEMO-002', 'tech_body_demo', 'Pass@1234', '13800000004', 'tech.body@example.com', 'BODY_WORK', 110, 0, 0),
  ('Diagnostic Demo', 'EMP-DEMO-003', 'tech_diag_demo', 'Pass@1234', '13800000005', 'tech.diag@example.com', 'DIAGNOSTIC', 130, 0, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  password = VALUES(password),
  phone = VALUES(phone),
  email = VALUES(email),
  skill_type = VALUES(skill_type),
  hourly_rate = VALUES(hourly_rate);

SET @admin_demo_id = (SELECT id FROM admin WHERE username = 'admin_demo' LIMIT 1);
SET @customer_demo_id = (SELECT id FROM user WHERE username = 'customer_demo' LIMIT 1);
SET @tech_demo_id = (SELECT id FROM technician WHERE username = 'tech_demo' LIMIT 1);
SET @tech_body_demo_id = (SELECT id FROM technician WHERE username = 'tech_body_demo' LIMIT 1);
SET @tech_diag_demo_id = (SELECT id FROM technician WHERE username = 'tech_diag_demo' LIMIT 1);

SET @demo_map_id = (SELECT id FROM journey_map WHERE enabled = 1 ORDER BY id LIMIT 1);
SET @demo_final_city_index = COALESCE((SELECT MAX(city_index) FROM journey_map_node WHERE map_id = @demo_map_id), 4);
SET @demo_checked_in_max = GREATEST(@demo_final_city_index - 1, 0);
SET @demo_next_city_index = @demo_final_city_index;
SET @demo_final_required_mileage = COALESCE(
  (SELECT required_mileage FROM journey_map_node WHERE map_id = @demo_map_id AND city_index = @demo_next_city_index LIMIT 1),
  580
);

-- 2) Cleanup old demo-prefixed data (idempotent re-run)
DELETE ot
FROM order_technician ot
JOIN repair_order ro ON ro.id = ot.order_id
WHERE ro.order_number LIKE 'DEMO2026-%';

DELETE f
FROM feedback f
JOIN repair_order ro ON ro.id = f.repair_order_id
WHERE ro.order_number LIKE 'DEMO2026-%';

DELETE FROM repair_order WHERE order_number LIKE 'DEMO2026-%';
DELETE FROM maintenance_alert WHERE dedup_key LIKE 'DEMO:%';
DELETE FROM inventory_alert_notification WHERE material_name LIKE 'DEMO-%';
DELETE FROM journey_footprint WHERE event_type LIKE 'DEMO_%';
DELETE FROM green_reward_ledger WHERE source_id LIKE 'DEMO_%';
DELETE FROM user_coupon_wallet WHERE source_action LIKE 'DEMO_%';
DELETE FROM journey_completion_record WHERE user_id = @customer_demo_id;
DELETE FROM green_journey_node_state WHERE user_id = @customer_demo_id;
DELETE FROM green_daily_quota WHERE user_id = @customer_demo_id;
DELETE FROM green_energy_account WHERE user_id = @customer_demo_id;
DELETE FROM material WHERE name LIKE 'DEMO-%';
DELETE FROM maintenance_alert WHERE user_id = @customer_demo_id;
DELETE FROM vehicle WHERE license_plate LIKE 'DEMO-%';

-- 3) Vehicles for customer demo
INSERT INTO vehicle (
  license_plate,
  brand,
  model,
  year,
  color,
  vin,
  current_mileage,
  last_maintenance_mileage,
  last_maintenance_at,
  user_id
) VALUES
  ('DEMO-A1001', 'Toyota', 'Camry', 2021, 'White', 'VINDEMOA1001000001', 12540, 1500, DATE_SUB(NOW(), INTERVAL 210 DAY), @customer_demo_id),
  ('DEMO-A1002', 'Tesla', 'Model 3', 2023, 'Black', 'VINDEMOA1002000002', 8340, 4100, DATE_SUB(NOW(), INTERVAL 90 DAY), @customer_demo_id);

SET @vehicle_demo_main = (SELECT id FROM vehicle WHERE license_plate = 'DEMO-A1001' ORDER BY id DESC LIMIT 1);
SET @vehicle_demo_backup = (SELECT id FROM vehicle WHERE license_plate = 'DEMO-A1002' ORDER BY id DESC LIMIT 1);

-- 4) Materials + inventory warning data
INSERT INTO material (name, unit_price, stock_quantity, minimum_stock_level)
VALUES
  ('DEMO-Engine-Oil', 188, 40, 20),
  ('DEMO-Brake-Pad', 320, 6, 20),
  ('DEMO-Coolant', 96, 0, 15),
  ('DEMO-Air-Filter', 120, 25, 10);

SET @mat_oil = (SELECT id FROM material WHERE name = 'DEMO-Engine-Oil' ORDER BY id DESC LIMIT 1);
SET @mat_brake = (SELECT id FROM material WHERE name = 'DEMO-Brake-Pad' ORDER BY id DESC LIMIT 1);
SET @mat_coolant = (SELECT id FROM material WHERE name = 'DEMO-Coolant' ORDER BY id DESC LIMIT 1);

-- 5) Repair orders for admin/tech/customer dashboards
INSERT INTO repair_order (
  order_number, status, description,
  created_at, updated_at, started_at, completed_at, repair_ended_at,
  labor_cost, estimated_emission, eco_material, rework_count, repair_type,
  material_cost, total_cost, estimated_hours, actual_hours,
  assignment_type, required_skill_type, urge_status,
  user_id, vehicle_id
) VALUES (
  'DEMO2026-001', 'COMPLETED', 'Engine noise and vibration check',
  DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY),
  420, 5.8, 1, 0, 'repair',
  260, 680, 3.0, 3.5,
  'AUTO', 'MECHANIC', 'NOT_URGED',
  @customer_demo_id, @vehicle_demo_main
);
SET @order_1 = (SELECT id FROM repair_order WHERE order_number = 'DEMO2026-001' LIMIT 1);

INSERT INTO repair_order (
  order_number, status, description,
  created_at, updated_at, started_at, completed_at, repair_ended_at,
  labor_cost, estimated_emission, eco_material, rework_count, repair_type,
  material_cost, total_cost, estimated_hours, actual_hours,
  assignment_type, required_skill_type, urge_status,
  user_id, vehicle_id
) VALUES (
  'DEMO2026-002', 'IN_PROGRESS', 'Brake response delay troubleshooting',
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NULL,
  NULL, 7.2, 0, 1, 'repair',
  NULL, NULL, 4.0, NULL,
  'AUTO', 'MECHANIC', 'NOT_URGED',
  @customer_demo_id, @vehicle_demo_main
);
SET @order_2 = (SELECT id FROM repair_order WHERE order_number = 'DEMO2026-002' LIMIT 1);

INSERT INTO repair_order (
  order_number, status, description,
  created_at, updated_at, started_at, completed_at, repair_ended_at,
  labor_cost, estimated_emission, eco_material, rework_count, repair_type,
  material_cost, total_cost, estimated_hours, actual_hours,
  assignment_type, required_skill_type, urge_status,
  user_id, vehicle_id
) VALUES (
  'DEMO2026-003', 'ASSIGNED', 'Starter system inspection',
  DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NULL, NULL,
  NULL, 4.4, 1, 0, 'repair',
  NULL, NULL, 2.5, NULL,
  'MANUAL', 'MECHANIC', 'NOT_URGED',
  @customer_demo_id, @vehicle_demo_main
);
SET @order_3 = (SELECT id FROM repair_order WHERE order_number = 'DEMO2026-003' LIMIT 1);

INSERT INTO repair_order (
  order_number, status, description,
  created_at, updated_at, started_at, completed_at, repair_ended_at,
  labor_cost, estimated_emission, eco_material, rework_count, repair_type,
  material_cost, total_cost, estimated_hours, actual_hours,
  assignment_type, required_skill_type, urge_status,
  user_id, vehicle_id
) VALUES (
  'DEMO2026-004', 'COMPLETED', 'Body paint restoration after scratch',
  DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY),
  220, 3.2, 1, 0, 'repair',
  180, 400, 2.0, 2.0,
  'AUTO', 'BODY_WORK', 'NOT_URGED',
  @customer_demo_id, @vehicle_demo_backup
);
SET @order_4 = (SELECT id FROM repair_order WHERE order_number = 'DEMO2026-004' LIMIT 1);

INSERT INTO repair_order (
  order_number, status, description,
  created_at, updated_at, started_at, completed_at, repair_ended_at,
  labor_cost, estimated_emission, eco_material, rework_count, repair_type,
  material_cost, total_cost, estimated_hours, actual_hours,
  assignment_type, required_skill_type, urge_status,
  user_id, vehicle_id
) VALUES (
  'DEMO2026-005', 'ASSIGNED', 'Rear bumper panel alignment',
  DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, NULL, NULL,
  NULL, 2.4, 1, 0, 'repair',
  NULL, NULL, 2.0, NULL,
  'AUTO', 'BODY_WORK', 'NOT_URGED',
  @customer_demo_id, @vehicle_demo_backup
);
SET @order_5 = (SELECT id FROM repair_order WHERE order_number = 'DEMO2026-005' LIMIT 1);

INSERT INTO repair_order (
  order_number, status, description,
  created_at, updated_at, started_at, completed_at, repair_ended_at,
  labor_cost, estimated_emission, eco_material, rework_count, repair_type,
  material_cost, total_cost, estimated_hours, actual_hours,
  assignment_type, required_skill_type, urge_status,
  user_id, vehicle_id
) VALUES (
  'DEMO2026-006', 'PENDING', 'Advanced sensor calibration diagnostics',
  DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL, NULL, NULL,
  NULL, 6.0, 0, 0, 'repair',
  NULL, NULL, 3.0, NULL,
  'AUTO', 'DIAGNOSTIC', 'NOT_URGED',
  @customer_demo_id, @vehicle_demo_backup
);
SET @order_6 = (SELECT id FROM repair_order WHERE order_number = 'DEMO2026-006' LIMIT 1);

INSERT INTO order_technician (order_id, technician_id)
VALUES
  (@order_1, @tech_demo_id),
  (@order_2, @tech_demo_id),
  (@order_3, @tech_demo_id),
  (@order_4, @tech_body_demo_id),
  (@order_5, @tech_body_demo_id);

INSERT INTO feedback (rating, comment, created_at, repair_order_id, user_id)
VALUES
  (5, 'DEMO: Fast and professional service.', DATE_SUB(NOW(), INTERVAL 8 DAY), @order_1, @customer_demo_id),
  (2, 'DEMO: Delay in delivery and poor communication.', DATE_SUB(NOW(), INTERVAL 3 DAY), @order_4, @customer_demo_id);

-- 6) Customer maintenance alerts
INSERT INTO maintenance_alert (
  user_id, vehicle_id, dedup_key, alert_type, message,
  trigger_time, status, created_at, updated_at
) VALUES
  (@customer_demo_id, @vehicle_demo_main, CONCAT('DEMO:', @customer_demo_id, ':', @vehicle_demo_main, ':MILEAGE_OVERDUE'), 'MILEAGE_OVERDUE',
   'DEMO: Mileage exceeded maintenance threshold, please schedule service soon.', DATE_SUB(NOW(), INTERVAL 3 DAY), 'UNREAD', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (@customer_demo_id, @vehicle_demo_backup, CONCAT('DEMO:', @customer_demo_id, ':', @vehicle_demo_backup, ':TIME_OVERDUE'), 'TIME_OVERDUE',
   'DEMO: More than 6 months since last maintenance check.', DATE_SUB(NOW(), INTERVAL 1 DAY), 'READ', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 7) Admin inventory notifications
INSERT INTO inventory_alert_notification (
  material_id, material_name, current_stock, minimum_stock_level, status, created_at, resolved_at
) VALUES
  (@mat_brake, 'DEMO-Brake-Pad', 6, 20, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
  (@mat_coolant, 'DEMO-Coolant', 0, 15, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL);

-- Keep technician aggregates aligned with order data shown on dispatch board
UPDATE technician t
SET
  t.total_work_hours = (
    SELECT COALESCE(SUM(CASE WHEN ro.actual_hours IS NULL THEN 0 ELSE ro.actual_hours END), 0)
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
WHERE t.id IN (@tech_demo_id, @tech_body_demo_id, @tech_diag_demo_id);

-- 8) Gamification near-final state for customer_demo
INSERT INTO green_energy_account (
  user_id,
  total_energy,
  current_mileage,
  current_map_id,
  journey_status,
  pending_random_quiz_id,
  frozen_mileage,
  random_event_next_retry_time,
  update_time,
  version
) VALUES (
  @customer_demo_id,
  860,
  @demo_final_required_mileage + 10,
  @demo_map_id,
  'NORMAL',
  NULL,
  0,
  NULL,
  NOW(),
  0
);

INSERT INTO green_daily_quota (user_id, quota_date, used_energy, update_time)
VALUES (@customer_demo_id, CURDATE(), 320, NOW())
ON DUPLICATE KEY UPDATE
  used_energy = VALUES(used_energy),
  update_time = VALUES(update_time);

INSERT INTO green_journey_node_state (
  user_id, map_id, city_index, node_state, checkin_at, next_retry_time, update_time, version
)
SELECT
  @customer_demo_id,
  @demo_map_id,
  n.city_index,
  CASE
    WHEN n.city_index <= @demo_checked_in_max THEN 'CHECKED_IN'
    WHEN n.city_index = @demo_next_city_index THEN 'UNLOCKED'
    ELSE 'LOCKED'
  END AS node_state,
  CASE
    WHEN n.city_index <= @demo_checked_in_max THEN DATE_SUB(NOW(), INTERVAL (10 - n.city_index) DAY)
    ELSE NULL
  END AS checkin_at,
  NULL,
  NOW(),
  0
FROM journey_map_node n
WHERE n.map_id = @demo_map_id
ORDER BY n.city_index;

INSERT INTO green_reward_ledger (
  user_id, source_type, source_id, action_key, energy_delta, mileage_delta, risk_level, reason, created_at
) VALUES
  (@customer_demo_id, 'REPAIR_ORDER', 'DEMO_ORDER_001', 'EMISSION_REWARD', 180, 180, 'LOW', 'DEMO order reward 001', DATE_SUB(NOW(), INTERVAL 9 DAY)),
  (@customer_demo_id, 'REPAIR_ORDER', 'DEMO_ORDER_004', 'EMISSION_REWARD', 120, 120, 'LOW', 'DEMO order reward 004', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (@customer_demo_id, 'JOURNEY_CITY', 'DEMO_CITY_0', 'CITY_CHECKIN_REWARD', 20, 20, 'LOW', 'DEMO city checkin 0', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@customer_demo_id, 'JOURNEY_CITY', 'DEMO_CITY_1', 'CITY_CHECKIN_REWARD', 22, 22, 'LOW', 'DEMO city checkin 1', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  (@customer_demo_id, 'JOURNEY_CITY', 'DEMO_CITY_2', 'CITY_CHECKIN_REWARD', 22, 22, 'LOW', 'DEMO city checkin 2', DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (@customer_demo_id, 'JOURNEY_CITY', 'DEMO_CITY_3', 'CITY_CHECKIN_REWARD', 22, 22, 'LOW', 'DEMO city checkin 3', DATE_SUB(NOW(), INTERVAL 5 DAY));

INSERT INTO journey_footprint (
  user_id, map_id, event_type, event_description, created_at
) VALUES
  (@customer_demo_id, @demo_map_id, 'DEMO_CHECKIN_CITY', 'Checked in city node 0', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@customer_demo_id, @demo_map_id, 'DEMO_CHECKIN_CITY', 'Checked in city node 1', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  (@customer_demo_id, @demo_map_id, 'DEMO_DRAW_COUPON', 'Won city coupon at node 2', DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (@customer_demo_id, @demo_map_id, 'DEMO_CHECKIN_CITY', 'Checked in city node 3', DATE_SUB(NOW(), INTERVAL 5 DAY));

SET @coupon_city_new = GREATEST(@demo_final_city_index - 1, 0);
SET @coupon_city_redeemed = GREATEST(@demo_final_city_index - 2, 0);
SET @coupon_new_id = (SELECT id FROM coupon WHERE city_index = @coupon_city_new ORDER BY id LIMIT 1);
SET @coupon_redeemed_id = (SELECT id FROM coupon WHERE city_index = @coupon_city_redeemed ORDER BY id LIMIT 1);

INSERT INTO user_coupon_wallet (
  user_id, coupon_id, brand_partner_id, city_index, coupon_title, coupon_description,
  coupon_status, source_action, draw_time, expire_time, redeem_time,
  redeem_shop_id, redeem_technician_id, update_time, version, map_id
)
SELECT
  @customer_demo_id,
  c.id,
  c.brand_partner_id,
  c.city_index,
  c.coupon_title,
  c.coupon_description,
  'NEW',
  'DEMO_WALLET_NEW',
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  COALESCE(c.expire_time, DATE_ADD(NOW(), INTERVAL 90 DAY)),
  NULL,
  NULL,
  NULL,
  NOW(),
  0,
  @demo_map_id
FROM coupon c
WHERE c.id = @coupon_new_id;

INSERT INTO user_coupon_wallet (
  user_id, coupon_id, brand_partner_id, city_index, coupon_title, coupon_description,
  coupon_status, source_action, draw_time, expire_time, redeem_time,
  redeem_shop_id, redeem_technician_id, update_time, version, map_id
)
SELECT
  @customer_demo_id,
  c.id,
  c.brand_partner_id,
  c.city_index,
  c.coupon_title,
  c.coupon_description,
  'REDEEMED',
  'DEMO_WALLET_REDEEMED',
  DATE_SUB(NOW(), INTERVAL 20 DAY),
  COALESCE(c.expire_time, DATE_ADD(NOW(), INTERVAL 60 DAY)),
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  c.brand_partner_id,
  @tech_demo_id,
  NOW(),
  0,
  @demo_map_id
FROM coupon c
WHERE c.id = @coupon_redeemed_id;

COMMIT;

-- 9) Quick verification output
SELECT 'admin' AS role, username, password FROM admin WHERE username = 'admin_demo'
UNION ALL
SELECT 'customer' AS role, username, password FROM user WHERE username = 'customer_demo'
UNION ALL
SELECT 'technician' AS role, username, password FROM technician WHERE username = 'tech_demo';

SELECT 'repair_order_demo_count' AS metric, COUNT(*) AS value
FROM repair_order
WHERE order_number LIKE 'DEMO2026-%'
UNION ALL
SELECT 'feedback_demo_count' AS metric, COUNT(*) AS value
FROM feedback f
JOIN repair_order ro ON ro.id = f.repair_order_id
WHERE ro.order_number LIKE 'DEMO2026-%'
UNION ALL
SELECT 'maintenance_alert_demo_count' AS metric, COUNT(*) AS value
FROM maintenance_alert
WHERE dedup_key LIKE 'DEMO:%'
UNION ALL
SELECT 'inventory_alert_demo_count' AS metric, COUNT(*) AS value
FROM inventory_alert_notification
WHERE material_name LIKE 'DEMO-%'
UNION ALL
SELECT 'journey_wallet_demo_count' AS metric, COUNT(*) AS value
FROM user_coupon_wallet
WHERE source_action LIKE 'DEMO_%';
