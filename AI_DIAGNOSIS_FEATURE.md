# 维修故障智能诊断功能 (Intelligent Diagnosis Feature)

## 功能概述 (Overview)

本功能为车辆维修管理系统添加了基于AI的智能故障诊断能力。用户在提交维修订单时，可以使用AI诊断功能来获取故障的初步分析、可能的原因、建议措施和预估费用。

This feature adds AI-powered intelligent fault diagnosis to the vehicle maintenance management system. Users can get preliminary fault analysis, possible causes, recommended actions, and estimated costs when submitting repair orders.

## 主要特性 (Key Features)

1. **智能故障分析** - 基于用户描述的故障现象，自动识别故障类型
2. **多领域诊断** - 支持发动机、刹车、电气、变速箱、车身外观、空调、轮胎等多个系统
3. **严重程度评估** - 自动评估故障的严重程度（低、中、高、极高）
4. **费用预估** - 提供维修费用的预估范围
5. **建议措施** - 给出详细的检查和维修建议
6. **技师类型推荐** - 自动推荐需要的维修技师类型

## 技术架构 (Technical Architecture)

### 后端 (Backend)

#### 1. DTOs (数据传输对象)
- `DiagnosisRequest.java` - 诊断请求
  - description: 故障描述
  - vehicleBrand: 车辆品牌
  - vehicleModel: 车辆型号
  - mileage: 里程数

- `DiagnosisResponse.java` - 诊断响应
  - faultType: 故障类型
  - possibleCause: 可能原因
  - recommendedActions: 建议措施列表
  - estimatedSeverity: 严重程度
  - estimatedCost: 预估费用
  - skillTypeRequired: 需要的技师类型

#### 2. 服务层
- `IntelligentDiagnosisService.java` - 智能诊断服务
  - 基于关键词识别的诊断逻辑
  - 支持多种故障类型的诊断
  - 可扩展以集成真实的AI API（如OpenAI、Claude等）

#### 3. 控制器
- `DiagnosisController.java` - 诊断API控制器
  - POST `/api/diagnosis/analyze` - 执行故障诊断

### 前端 (Frontend)

#### 1. Vue组件修改
在 `CustomerDashboard.vue` 中添加：
- AI诊断按钮
- 诊断结果展示卡片
- 诊断加载状态
- 响应式UI设计

#### 2. UI特性
- 渐变色AI诊断按钮
- 动画展示诊断结果
- 严重程度颜色编码
- 建议措施列表展示
- 自动填充推荐的维修类型

## 使用方式 (Usage)

### 用户操作流程
1. 用户在客户端仪表板点击"预约维修"
2. 选择车辆并填写故障描述
3. 点击"AI智能诊断"按钮
4. 系统显示诊断结果，包括：
   - 故障类型和可能原因
   - 严重程度（带颜色标识）
   - 预估维修费用
   - 详细的建议措施
   - 推荐的维修类型
5. 系统自动填充推荐的维修类型
6. 用户确认后提交维修订单

### API使用示例

```bash
curl -X POST http://localhost:8080/api/diagnosis/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "description": "发动机启动不了，打不着火",
    "vehicleBrand": "大众",
    "vehicleModel": "速腾",
    "mileage": 50000
  }'
```

响应示例:
```json
{
  "faultType": "发动机故障",
  "possibleCause": "电池电量不足、启动马达故障或点火系统故障",
  "recommendedActions": [
    "检查电池电量和接线",
    "检查启动马达工作状况",
    "检查火花塞和点火线圈",
    "检查燃油供应系统"
  ],
  "estimatedSeverity": "高",
  "estimatedCost": 800.0,
  "skillTypeRequired": "MECHANIC"
}
```

## 支持的故障类型 (Supported Fault Types)

1. **发动机系统**
   - 启动故障
   - 怠速不稳
   - 异常熄火

2. **刹车系统**
   - 制动距离长
   - 异响噪音
   - 刹车失灵

3. **电气系统**
   - 电池问题
   - 灯光故障
   - 充电问题

4. **变速箱**
   - 换挡困难
   - 异响

5. **车身外观**
   - 碰撞凹陷
   - 划痕掉漆

6. **空调系统**
   - 制冷制热问题

7. **轮胎**
   - 爆胎漏气
   - 磨损

## 扩展建议 (Future Enhancements)

### 集成真实AI API
当前实现使用基于关键词的规则引擎。可以通过以下方式集成真实的AI服务：

1. **OpenAI GPT集成**
```java
// 在 IntelligentDiagnosisService.java 中
private String callOpenAI(String description) {
    // 调用 OpenAI API
    // 使用 prompt engineering 来获取结构化的诊断结果
}
```

2. **Azure认知服务**
3. **自建机器学习模型**
4. **集成汽车故障数据库**

### 数据收集与改进
- 收集用户反馈
- 记录诊断准确性
- 使用历史维修数据训练模型
- 持续优化诊断算法

### 多语言支持
- 添加英语、日语等多语言支持
- 国际化故障描述

## 文件清单 (File List)

### 后端文件
- `backend/src/main/java/org/com/repair/DTO/DiagnosisRequest.java`
- `backend/src/main/java/org/com/repair/DTO/DiagnosisResponse.java`
- `backend/src/main/java/org/com/repair/controller/DiagnosisController.java`
- `backend/src/main/java/org/com/repair/service/IntelligentDiagnosisService.java`

### 前端文件
- `frontend/src/views/CustomerDashboard.vue` (修改)

## 测试 (Testing)

### 后端测试
1. 编译后端：
```bash
cd backend
mvn clean compile
```

2. 运行后端服务：
```bash
mvn spring-boot:run
```

3. 使用测试脚本：
```bash
/tmp/test_diagnosis_api.sh
```

### 前端测试
1. 启动前端开发服务器
2. 登录为用户角色
3. 点击"预约维修"
4. 填写故障描述后点击"AI智能诊断"按钮
5. 验证诊断结果显示正确

## 注意事项 (Notes)

1. 当前版本使用规则引擎而非真实AI API，适合演示和原型开发
2. 生产环境建议集成真实的AI服务以获得更准确的诊断结果
3. 诊断结果仅供参考，实际维修应由专业技师进行
4. 预估费用基于一般情况，实际费用可能有所不同

## 配置 (Configuration)

如需集成真实的AI API，可在 `application.properties` 中添加：

```properties
# AI API Configuration (示例)
ai.api.enabled=true
ai.api.endpoint=https://api.openai.com/v1/chat/completions
ai.api.key=${OPENAI_API_KEY}
ai.api.model=gpt-4
```

## 许可 (License)

本功能遵循项目的整体许可协议。
