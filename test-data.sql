-- 插入用户数据
INSERT INTO user (id, username, password, name, phone, email, address) VALUES
(1, 'user1', 'password1', '张伟', '13800138001', 'user1@example.com', '北京市朝阳区建国路1号'),
(2, 'user2', 'password2', '李芳', '13800138002', 'user2@example.com', '上海市浦东新区人民路2号');

-- 插入车辆数据
INSERT INTO vehicle (id, license_plate, brand, model, year, color, vin, user_id) VALUES
(1, '京A12345', '丰田', '卡罗拉', 2018, '白色', 'VIN123456789012345', 1),
(2, '沪B67890', '本田', '思域', 2020, '黑色', 'VIN987654321098765', 2);

-- 插入技师数据
INSERT INTO technician (id, name, employee_id, username, password, phone, email, skill_type, hourly_rate, total_work_hours, completed_orders) VALUES
(1, '王强', 'EMP001', 'tech1', 'password1', '13800138003', 'tech1@example.com', 'MECHANIC', 80.0, 0.0, 0),
(2, '赵敏', 'EMP002', 'tech2', 'password2', '13800138004', 'tech2@example.com', 'ELECTRICIAN', 85.0, 0.0, 0);

-- 插入维修订单数据
INSERT INTO repair_order (id, order_number, status, description, created_at, updated_at, completed_at, labor_cost, material_cost, total_cost, estimated_hours, actual_hours, user_id, vehicle_id, required_skill_type, assignment_type) VALUES
(1, 'RO20231001', 'COMPLETED', '发动机启动困难，怠速不稳', '2023-10-01 10:00:00', '2023-10-01 12:00:00', '2023-10-01 14:00:00', 160.0, 200.0, 360.0, 2.0, 2.0, 1, 1, 'MECHANIC', 'AUTO'),
(2, 'RO20231002', 'PENDING', '空调不制冷，可能是氟利昂不足', '2023-10-02 09:00:00', '2023-10-02 09:00:00', NULL, NULL, NULL, NULL, 3.0, NULL, 2, 2, 'ELECTRICIAN', 'AUTO');

-- 插入反馈数据
INSERT INTO feedback (id, rating, comment, created_at, repair_order_id, user_id) VALUES
(1, 5, '服务态度非常好，维修质量也很满意', '2023-10-01 15:00:00', 1, 1),
(2, 4, '修理速度快，价格合理', '2023-10-02 10:00:00', 2, 2);

-- 插入管理员数据
INSERT INTO admin (id, username, password, name, phone, email, role) VALUES
(1, 'admin', 'admin', '超级管理员', '13800138000', 'admin@example.com', 'SUPER_ADMIN'),
(2, 'manager1', 'password', '李经理', '13800138005', 'manager1@example.com', 'MANAGER');
