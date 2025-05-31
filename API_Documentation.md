# 汽车维修管理系统 API 文档

## 概述

本文档描述了汽车维修管理系统的后端API接口。系统提供用户管理、管理员管理、车辆管理、技师管理、维修订单管理、材料管理和反馈管理等功能。

**基础URL:** `http://localhost:8080`

---

## 1. 用户管理 API (`/api/users`)

### 1.1 用户注册
- **接口:** `POST /api/users`
- **描述:** 注册新用户
- **请求体:**
```json
{
  "username": "用户名",
  "password": "密码",
  "name": "姓名",
  "phone": "电话号码",
  "email": "邮箱地址",
  "address": "地址",
  "vehicles": [],
  "repairOrders": []
}
```
- **响应:** `201 Created`
- **返回数据:** UserResponse 对象

### 1.2 根据ID获取用户
- **接口:** `GET /api/users/{id}`
- **描述:** 根据用户ID获取用户信息
- **路径参数:** `id` - 用户ID
- **响应:** `200 OK` 或 `404 Not Found`

### 1.3 根据用户名获取用户
- **接口:** `GET /api/users/by-username/{username}`
- **描述:** 根据用户名获取用户信息
- **路径参数:** `username` - 用户名
- **响应:** `200 OK` 或 `404 Not Found`

### 1.4 获取所有用户
- **接口:** `GET /api/users`
- **描述:** 获取所有用户列表
- **响应:** `200 OK`
- **返回数据:** UserResponse 数组

### 1.5 更新用户信息
- **接口:** `PUT /api/users/{id}`
- **描述:** 更新用户信息
- **路径参数:** `id` - 用户ID
- **请求体:** NewUserRequest 对象
- **响应:** `200 OK` 或 `404 Not Found`

### 1.6 删除用户
- **接口:** `DELETE /api/users/{id}`
- **描述:** 删除用户
- **路径参数:** `id` - 用户ID
- **响应:** `204 No Content` 或 `404 Not Found`

### 1.7 用户登录
- **接口:** `POST /api/users/login`
- **描述:** 用户登录验证
- **查询参数:**
  - `username` - 用户名
  - `password` - 密码
- **响应:** `200 OK` 或 `401 Unauthorized`

---

## 2. 管理员管理 API (`/api/admins`)

### 2.1 管理员注册
- **接口:** `POST /api/admins`
- **描述:** 注册新管理员
- **请求体:**
```json
{
  "username": "管理员用户名",
  "password": "密码",
  "role": "角色"
}
```
- **响应:** `201 Created`

### 2.2 根据ID获取管理员
- **接口:** `GET /api/admins/{id}`
- **描述:** 根据管理员ID获取管理员信息
- **路径参数:** `id` - 管理员ID
- **响应:** `200 OK` 或 `404 Not Found`

### 2.3 根据用户名获取管理员
- **接口:** `GET /api/admins/by-username/{username}`
- **描述:** 根据用户名获取管理员信息
- **路径参数:** `username` - 管理员用户名
- **响应:** `200 OK` 或 `404 Not Found`

### 2.4 根据角色获取管理员
- **接口:** `GET /api/admins/by-role/{role}`
- **描述:** 根据角色获取管理员信息
- **路径参数:** `role` - 角色
- **响应:** `200 OK` 或 `404 Not Found`

### 2.5 获取所有管理员
- **接口:** `GET /api/admins`
- **描述:** 获取所有管理员列表
- **响应:** `200 OK`

### 2.6 更新管理员信息
- **接口:** `PUT /api/admins/{id}`
- **描述:** 更新管理员信息
- **路径参数:** `id` - 管理员ID
- **请求体:** NewAdminRequest 对象
- **响应:** `200 OK` 或 `404 Not Found`

### 2.7 删除管理员
- **接口:** `DELETE /api/admins/{id}`
- **描述:** 删除管理员
- **路径参数:** `id` - 管理员ID
- **响应:** `204 No Content` 或 `404 Not Found`

### 2.8 管理员登录
- **接口:** `POST /api/admins/login`
- **描述:** 管理员登录验证
- **查询参数:**
  - `username` - 用户名
  - `password` - 密码
- **响应:** `200 OK` 或 `401 Unauthorized`

---

## 3. 车辆管理 API (`/api/vehicles`)

### 3.1 添加车辆
- **接口:** `POST /api/vehicles`
- **描述:** 添加新车辆
- **请求体:**
```json
{
  "licensePlate": "车牌号",
  "model": "车型",
  "year": "年份",
  "userId": "车主ID"
}
```
- **响应:** `201 Created`

### 3.2 根据ID获取车辆
- **接口:** `GET /api/vehicles/{id}`
- **描述:** 根据车辆ID获取车辆信息
- **路径参数:** `id` - 车辆ID
- **响应:** `200 OK` 或 `404 Not Found`

### 3.3 根据用户ID获取车辆
- **接口:** `GET /api/vehicles/user/{userId}`
- **描述:** 获取指定用户的所有车辆
- **路径参数:** `userId` - 用户ID
- **响应:** `200 OK`

### 3.4 根据车牌号获取车辆
- **接口:** `GET /api/vehicles/license-plate/{licensePlate}`
- **描述:** 根据车牌号获取车辆信息
- **路径参数:** `licensePlate` - 车牌号
- **响应:** `200 OK` 或 `404 Not Found`

### 3.5 获取所有车辆
- **接口:** `GET /api/vehicles`
- **描述:** 获取所有车辆列表
- **响应:** `200 OK`

### 3.6 更新车辆信息
- **接口:** `PUT /api/vehicles/{id}`
- **描述:** 更新车辆信息
- **路径参数:** `id` - 车辆ID
- **请求体:** NewVehicleRequest 对象
- **响应:** `200 OK` 或 `404 Not Found`

### 3.7 删除车辆
- **接口:** `DELETE /api/vehicles/{id}`
- **描述:** 删除车辆
- **路径参数:** `id` - 车辆ID
- **响应:** `204 No Content` 或 `404 Not Found`

### 3.8 按车型统计维修数据
- **接口:** `GET /api/vehicles/statistics/by-model`
- **描述:** 获取按车型分组的维修统计数据
- **响应:** `200 OK`

---

## 4. 技师管理 API (`/api/technicians`)

### 4.1 添加技师
- **接口:** `POST /api/technicians`
- **描述:** 添加新技师
- **请求体:**
```json
{
  "name": "姓名",
  "employeeId": "员工ID",
  "username": "用户名",
  "password": "密码",
  "phone": "电话号码",
  "email": "邮箱",
  "skillType": "技能类型",
  "hourlyRate": "小时费率"
}
```
- **响应:** `201 Created`

### 4.2 根据ID获取技师
- **接口:** `GET /api/technicians/{id}`
- **描述:** 根据技师ID获取技师信息
- **路径参数:** `id` - 技师ID
- **响应:** `200 OK` 或 `404 Not Found`

### 4.3 根据员工ID获取技师
- **接口:** `GET /api/technicians/employee/{employeeId}`
- **描述:** 根据员工ID获取技师信息
- **路径参数:** `employeeId` - 员工ID
- **响应:** `200 OK` 或 `404 Not Found`

### 4.4 获取所有技师
- **接口:** `GET /api/technicians`
- **描述:** 获取所有技师列表
- **响应:** `200 OK`

### 4.5 根据技能类型获取技师
- **接口:** `GET /api/technicians/skill/{skillType}`
- **描述:** 根据技能类型获取技师列表
- **路径参数:** `skillType` - 技能类型 (MECHANICAL, ELECTRICAL, BODYWORK, PAINTING)
- **响应:** `200 OK`

### 4.6 根据小时费率范围获取技师
- **接口:** `GET /api/technicians/hourly-rate`
- **描述:** 根据小时费率范围获取技师
- **查询参数:**
  - `minRate` - 最低费率
  - `maxRate` - 最高费率
- **响应:** `200 OK`

### 4.7 获取可用技师
- **接口:** `GET /api/technicians/available`
- **描述:** 获取可用技师列表
- **查询参数:**
  - `skillType` (可选) - 技能类型
- **响应:** `200 OK`

### 4.8 更新技师信息
- **接口:** `PUT /api/technicians/{id}`
- **描述:** 更新技师信息
- **路径参数:** `id` - 技师ID
- **请求体:** NewTechnicianRequest 对象
- **响应:** `200 OK` 或 `404 Not Found`

### 4.9 删除技师
- **接口:** `DELETE /api/technicians/{id}`
- **描述:** 删除技师
- **路径参数:** `id` - 技师ID
- **响应:** `204 No Content` 或 `404 Not Found`

### 4.10 按技能类型统计技师数量
- **接口:** `GET /api/technicians/statistics/by-skill-type`
- **描述:** 获取按技能类型分组的技师数量统计
- **响应:** `200 OK`

### 4.11 计算技师总收入
- **接口:** `GET /api/technicians/{id}/earnings`
- **描述:** 计算技师的总收入
- **路径参数:** `id` - 技师ID
- **响应:** `200 OK` 或 `404 Not Found`

### 4.12 技师登录
- **接口:** `POST /api/technicians/login`
- **描述:** 技师登录验证
- **查询参数:**
  - `username` - 用户名
  - `password` - 密码
- **响应:** `200 OK` 或 `401 Unauthorized`

---

## 5. 维修订单管理 API (`/api/repair-orders`)

### 5.1 创建维修订单
- **接口:** `POST /api/repair-orders`
- **描述:** 创建新的维修订单
- **请求体:**
```json
{
  "userId": "用户ID",
  "vehicleId": "车辆ID",
  "technicianId": "技师ID",
  "description": "故障描述",
  "startDate": "开始日期",
  "endDate": "结束日期",
  "estimatedCost": "预估费用",
  "actualCost": "实际费用",
  "status": "订单状态"
}
```
- **响应:** `201 Created`

### 5.2 根据ID获取维修订单
- **接口:** `GET /api/repair-orders/{id}`
- **描述:** 根据订单ID获取维修订单信息
- **路径参数:** `id` - 订单ID
- **响应:** `200 OK` 或 `404 Not Found`

### 5.3 根据用户ID获取维修订单
- **接口:** `GET /api/repair-orders/user/{userId}`
- **描述:** 获取指定用户的所有维修订单
- **路径参数:** `userId` - 用户ID
- **响应:** `200 OK`

### 5.4 根据车辆ID获取维修订单
- **接口:** `GET /api/repair-orders/vehicle/{vehicleId}`
- **描述:** 获取指定车辆的所有维修订单
- **路径参数:** `vehicleId` - 车辆ID
- **响应:** `200 OK`

### 5.5 根据状态获取维修订单
- **接口:** `GET /api/repair-orders/status/{status}`
- **描述:** 根据订单状态获取维修订单
- **路径参数:** `status` - 订单状态 (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)
- **响应:** `200 OK`

### 5.6 获取未完成的维修订单
- **接口:** `GET /api/repair-orders/uncompleted`
- **描述:** 获取所有未完成的维修订单
- **响应:** `200 OK`

### 5.7 获取所有维修订单
- **接口:** `GET /api/repair-orders`
- **描述:** 获取所有维修订单列表
- **响应:** `200 OK`

### 5.8 更新维修订单状态
- **接口:** `PUT /api/repair-orders/{id}/status`
- **描述:** 更新维修订单状态
- **路径参数:** `id` - 订单ID
- **查询参数:** `status` - 新状态
- **响应:** `200 OK` 或 `404 Not Found`

### 5.9 更新维修订单
- **接口:** `PUT /api/repair-orders/{id}`
- **描述:** 更新维修订单信息
- **路径参数:** `id` - 订单ID
- **请求体:** NewRepairOrderRequest 对象
- **响应:** `200 OK` 或 `404 Not Found`

### 5.10 删除维修订单
- **接口:** `DELETE /api/repair-orders/{id}`
- **描述:** 删除维修订单
- **路径参数:** `id` - 订单ID
- **响应:** `204 No Content` 或 `404 Not Found`

### 5.11 季度成本分析
- **接口:** `GET /api/repair-orders/analysis/quarterly`
- **描述:** 获取季度成本分析数据
- **查询参数:**
  - `year` - 年份
  - `quarter` - 季度
- **响应:** `200 OK`

### 5.12 月度成本分析
- **接口:** `GET /api/repair-orders/analysis/monthly`
- **描述:** 获取月度成本分析数据
- **查询参数:**
  - `year` - 年份
  - `month` - 月份
- **响应:** `200 OK`

### 5.13 获取负面反馈的订单
- **接口:** `GET /api/repair-orders/negative-feedback`
- **描述:** 获取具有负面反馈的订单
- **查询参数:** `maxRating` (默认值: 3) - 最大评分
- **响应:** `200 OK`

### 5.14 按技能类型统计任务
- **接口:** `GET /api/repair-orders/task-statistics`
- **描述:** 按技能类型统计任务数据
- **查询参数:**
  - `startDate` - 开始日期 (格式: yyyy-MM-dd)
  - `endDate` - 结束日期 (格式: yyyy-MM-dd)
- **响应:** `200 OK`

### 5.15 手动重新分配技师
- **接口:** `PUT /api/repair-orders/{id}/reassign`
- **描述:** 手动重新分配技师到指定维修订单
- **路径参数:** `id` - 订单ID
- **查询参数:** `isManual` (默认值: true) - 是否为手动分配
- **请求体:** 技师ID数组
```json
[1, 2, 3]
```
- **响应:** `200 OK` 或 `404 Not Found`

### 5.16 自动重新分配技师
- **接口:** `PUT /api/repair-orders/{id}/auto-reassign`
- **描述:** 根据系统算法自动重新分配技师
- **路径参数:** `id` - 订单ID
- **响应:** `200 OK` 或 `404 Not Found`

---

## 6. 材料管理 API (`/api/materials`)

### 6.1 添加材料
- **接口:** `POST /api/materials`
- **描述:** 添加新材料
- **请求体:**
```json
{
  "name": "材料名称",
  "price": "价格",
  "quantity": "数量",
  "repairOrderId": "维修订单ID"
}
```
- **响应:** `201 Created`

### 6.2 根据ID获取材料
- **接口:** `GET /api/materials/{id}`
- **描述:** 根据材料ID获取材料信息
- **路径参数:** `id` - 材料ID
- **响应:** `200 OK` 或 `404 Not Found`

### 6.3 根据维修订单ID获取材料
- **接口:** `GET /api/materials/repair-order/{repairOrderId}`
- **描述:** 获取指定维修订单的所有材料
- **路径参数:** `repairOrderId` - 维修订单ID
- **响应:** `200 OK`

### 6.4 获取所有材料
- **接口:** `GET /api/materials`
- **描述:** 获取所有材料列表
- **响应:** `200 OK`

### 6.5 根据名称搜索材料
- **接口:** `GET /api/materials/search`
- **描述:** 根据材料名称搜索材料
- **查询参数:** `name` - 材料名称
- **响应:** `200 OK`

### 6.6 根据价格范围获取材料
- **接口:** `GET /api/materials/price-range`
- **描述:** 根据价格范围获取材料
- **查询参数:**
  - `minPrice` - 最低价格
  - `maxPrice` - 最高价格
- **响应:** `200 OK`

### 6.7 更新材料信息
- **接口:** `PUT /api/materials/{id}`
- **描述:** 更新材料信息
- **路径参数:** `id` - 材料ID
- **请求体:** NewMaterialRequest 对象
- **响应:** `200 OK` 或 `404 Not Found`

### 6.8 删除材料
- **接口:** `DELETE /api/materials/{id}`
- **描述:** 删除材料
- **路径参数:** `id` - 材料ID
- **响应:** `204 No Content` 或 `404 Not Found`

### 6.9 计算总材料成本
- **接口:** `GET /api/materials/cost/{repairOrderId}`
- **描述:** 计算指定维修订单的总材料成本
- **路径参数:** `repairOrderId` - 维修订单ID
- **响应:** `200 OK` 或 `404 Not Found`

### 6.10 获取最常用材料
- **接口:** `GET /api/materials/most-used`
- **描述:** 获取使用最频繁的材料统计
- **响应:** `200 OK`

### 6.11 按维修类型获取最常用材料
- **接口:** `GET /api/materials/most-used-by-repair-type`
- **描述:** 按维修类型获取最常用的材料
- **查询参数:** `description` - 维修描述/类型
- **响应:** `200 OK`

---

## 7. 反馈管理 API (`/api/feedbacks`)

### 7.1 添加反馈
- **接口:** `POST /api/feedbacks`
- **描述:** 添加新反馈
- **请求体:**
```json
{
  "repairOrderId": "维修订单ID",
  "userId": "用户ID",
  "rating": "评分",
  "comment": "评论"
}
```
- **响应:** `201 Created`

### 7.2 根据ID获取反馈
- **接口:** `GET /api/feedbacks/{id}`
- **描述:** 根据反馈ID获取反馈信息
- **路径参数:** `id` - 反馈ID
- **响应:** `200 OK` 或 `404 Not Found`

### 7.3 根据维修订单ID获取反馈
- **接口:** `GET /api/feedbacks/repair-order/{repairOrderId}`
- **描述:** 获取指定维修订单的所有反馈
- **路径参数:** `repairOrderId` - 维修订单ID
- **响应:** `200 OK`

### 7.4 根据用户ID获取反馈
- **接口:** `GET /api/feedbacks/user/{userId}`
- **描述:** 获取指定用户的所有反馈
- **路径参数:** `userId` - 用户ID
- **响应:** `200 OK`

### 7.5 获取所有反馈
- **接口:** `GET /api/feedbacks`
- **描述:** 获取所有反馈列表
- **响应:** `200 OK`

### 7.6 根据评分范围获取反馈
- **接口:** `GET /api/feedbacks/rating-range`
- **描述:** 根据评分范围获取反馈
- **查询参数:**
  - `minRating` - 最低评分
  - `maxRating` - 最高评分
- **响应:** `200 OK`

### 7.7 根据关键词搜索反馈
- **接口:** `GET /api/feedbacks/search`
- **描述:** 根据关键词搜索反馈内容
- **查询参数:** `keyword` - 搜索关键词
- **响应:** `200 OK`

### 7.8 更新反馈
- **接口:** `PUT /api/feedbacks/{id}`
- **描述:** 更新反馈信息
- **路径参数:** `id` - 反馈ID
- **请求体:** NewFeedbackRequest 对象
- **响应:** `200 OK` 或 `404 Not Found`

### 7.9 删除反馈
- **接口:** `DELETE /api/feedbacks/{id}`
- **描述:** 删除反馈
- **路径参数:** `id` - 反馈ID
- **查询参数:** `userId` - 用户ID (用于权限验证)
- **响应:** `204 No Content`, `404 Not Found` 或 `403 Forbidden`

### 7.10 计算平均评分
- **接口:** `GET /api/feedbacks/average-rating/{repairOrderId}`
- **描述:** 计算指定维修订单的平均评分
- **路径参数:** `repairOrderId` - 维修订单ID
- **响应:** `200 OK` 或 `404 Not Found`

### 7.11 计算技师平均评分
- **接口:** `GET /api/feedbacks/technician-rating/{technicianId}`
- **描述:** 计算指定技师的平均评分
- **路径参数:** `technicianId` - 技师ID
- **响应:** `200 OK` 或 `404 Not Found`

### 7.12 获取评分分布
- **接口:** `GET /api/feedbacks/rating-distribution`
- **描述:** 获取评分分布统计
- **响应:** `200 OK`

---

## 状态码说明

- **200 OK:** 请求成功
- **201 Created:** 资源创建成功
- **204 No Content:** 删除成功，无返回内容
- **400 Bad Request:** 请求参数错误
- **401 Unauthorized:** 认证失败
- **403 Forbidden:** 权限不足
- **404 Not Found:** 资源不存在
- **500 Internal Server Error:** 服务器内部错误

## 数据格式

所有API接口均使用JSON格式进行数据交换。请求头应包含：
```
Content-Type: application/json
```

## 枚举类型

### RepairStatus (维修状态)
- `PENDING` - 待处理
- `IN_PROGRESS` - 进行中
- `COMPLETED` - 已完成
- `CANCELLED` - 已取消

### SkillType (技能类型)
- `MECHANICAL` - 机械维修
- `ELECTRICAL` - 电气维修
- `BODYWORK` - 车身维修
- `PAINTING` - 喷漆

---

**最后更新时间:** 2024年12月