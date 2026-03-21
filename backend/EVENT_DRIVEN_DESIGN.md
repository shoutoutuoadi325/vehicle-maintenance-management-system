# 事件驱动解耦设计：业务向游戏化的单向能量注入

## 设计目标
将主业务模块（维保工单完成）与游戏化模块（绿色能量奖励）通过事件驱动机制解耦，实现"不修改原核心逻辑、单向能量注入"的架构纪律。

## 架构设计图

```
RepairOrderService（维保工单服务）
    └─ updateRepairOrderStatus()
        └─ [订单状态变为 COMPLETED 时]
            └─ 发布：EmissionReducedEvent
                ↓
                └─→ GamificationService（游戏化服务）
                    └─ @EventListener handleEmissionReduced()
                        └─ 读取减排量
                        └─ 按比例转化为绿色能量
                        └─ 更新 GreenEnergyAccount
```

## 实现细节

### 1. 事件类：EmissionReducedEvent

**文件路径：** `backend/src/main/java/org/com/repair/event/EmissionReducedEvent.java`

**关键属性：**
- `repairOrderId`：触发事件的维修工单 ID
- `userId`：受益用户 ID
- `emissionReduction`：碳减排量（单位：kg CO2）

**特点：**
- 继承 Spring 的 `ApplicationEvent`
- 包含完整的事件上下文信息
- 易于后续扩展（日志、审计等）

### 2. 事件发布：RepairOrderService

**修改点：** `updateRepairOrderStatus()` 方法

**发布逻辑：**
```java
// 当订单从 IN_PROGRESS 变为 COMPLETED 时
if (oldStatus == RepairStatus.IN_PROGRESS && newStatus == RepairStatus.COMPLETED) {
    // ... 现有逻辑（计算费用、更新技师完成数等）
    
    // 新增：发布减排事件
    Double emissionReduction = repairOrder.getEstimatedEmission();
    if (emissionReduction != null && emissionReduction > 0 && repairOrder.getUser() != null) {
        EmissionReducedEvent event = new EmissionReducedEvent(
            this,
            repairOrder.getId(),
            repairOrder.getUser().getId(),
            emissionReduction
        );
        eventPublisher.publishEvent(event);
    }
}
```

**关键点：**
- 仅在减排量有效且用户存在时发布
- 不抛出异常，确保主业务不受影响
- 异步处理（可配合 @Async 进一步提升性能）

### 3. 事件监听：GamificationService

**新增方法：** `handleEmissionReduced()`

**功能：**
```java
@EventListener
@Transactional
public void handleEmissionReduced(EmissionReducedEvent event) {
    // 1. 提取事件信息
    Long userId = event.getUserId();
    Double emissionReduction = event.getEmissionReduction();
    
    // 2. 获取或创建用户绿色能量账户
    GreenEnergyAccount account = getOrCreateUserAccount(userId);
    
    // 3. 能量转化比例：1kg 减排量 = 100 绿色能量
    int energyReward = (int) Math.round(emissionReduction * 100.0);
    
    // 4. 更新账户
    account.setTotalEnergy(account.getTotalEnergy() + energyReward);
    account.setCurrentMileage(account.getCurrentMileage() + energyReward);
    greenEnergyAccountRepository.save(account);
    
    // 5. 日志记录
    logger.info("用户 {} 因维修工单 {} 获得能量奖励：{}",
        userId, event.getRepairOrderId(), energyReward);
}
```

**关键特性：**
- `@EventListener`：自动注册为事件监听器
- `@Transactional`：保证数据一致性
- 异常捕获：防止事件处理失败影响系统
- 日志完整：方便业务追踪和调试

## 调用流程

### 场景：技师完成维修工单

1. **控制器调用：** `RepairOrderController.updateRepairOrderStatus()`
2. **服务处理：** `RepairOrderService.updateRepairOrderStatus()`
   - 计算费用、更新技师状态等现有逻辑
   - **发布 EmissionReducedEvent**
3. **事件传播：** Spring 事件框架
4. **监听执行：** `GamificationService.handleEmissionReduced()`
   - 获取用户账户
   - 计算能量奖励（减排量 × 100）
   - 更新绿色能量和里程

### 数据流

```
RepairOrder (完成状态)
    ├─ estimatedEmission: 2.5 kg CO2
    └─ user.id: 1001
            ↓
    EmissionReducedEvent(1001, 2.5)
            ↓
    GreenEnergyAccount (用户 1001)
    ├─ totalEnergy: 保留值 + 250
    └─ currentMileage: 保留值 + 250
```

## 能量转化系数

| 减排量 (kg CO2) | 能量奖励 | 备注 |
|---|---|---|
| 1.0 | 100 | 标准系数 1:100 |
| 2.5 | 250 | |
| 5.0 | 500 | 使用环保材料可达到 |

**配置位置：** `GamificationService.EMISSION_TO_ENERGY_RATIO`

## 优势

### 1. **高度解耦**
   - RepairOrderService 无需感知 GamificationService
   - 修改或移除游戏化模块不影响主业务

### 2. **易于测试**
   - 可独立测试事件发布逻辑
   - 可独立测试事件监听逻辑
   - 事件本身是可序列化的简单 POJO

### 3. **可扩展**
   - 添加新的监听器只需实现 `@EventListener`
   - 无需修改事件发布代码
   - 支持多个监听器并行处理

### 4. **异常隔离**
   - 事件处理异常不会影响主业务的数据操作
   - 完整的异常日志便于问题排查

## 配置要求

### 依赖
- Spring Framework 4.2+（内置事件机制）
- 当前项目使用 Spring Boot 3.4.5，完全支持

### 启用异步处理（可选）
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
}

// 在监听器上添加
@EventListener
@Async("eventExecutor")
public void handleEmissionReduced(EmissionReducedEvent event) {
    // ...
}
```

## 监控与日志

### 关键日志输出

```log
[INFO] 维修工单 12345 完成，用户 1001 获得绿色能量奖励：减排量 2.50kg -> 能量 250 点，当前总能量：1250，当前里程：580
```

### 故障诊断

| 现象 | 可能原因 | 解决方案 |
|---|---|---|
| 能量未更新 | 事件未被发布 | 检查 estimatedEmission 是否为空 |
| 能量重复更新 | 事件被处理多次 | 检查 @EventListener 重复注册 |
| 事件处理缓慢 | 同步处理阻塞 | 启用 @Async 异步处理 |

## 后续扩展建议

1. **事件溯源**：记录所有 EmissionReducedEvent 到日志表，便于审计
2. **分析面板**：显示用户能量来源分布（问答 vs 维保）
3. **排行榜**：基于 currentMileage 构建实时排行
4. **成就系统**：到达指定里程时解锁徽章
5. **积分商城**：用绿色能量兑换实物奖励或优惠券

