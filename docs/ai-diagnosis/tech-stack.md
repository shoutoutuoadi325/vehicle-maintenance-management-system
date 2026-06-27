# AI 维修模块技术栈与架构约束

## 1. 现有技术基础

### 1.1 后端

- Java
- Spring Boot
- Spring MVC REST API
- Spring Data JPA
- MySQL
- Flyway 数据库迁移
- OkHttp 调用外部 AI 服务
- Jackson 处理 JSON

现有 AI 入口：

```text
AIDiagnosisController
-> AIDiagnosisService
-> 外部 Chat Completions API / 本地规则兜底
```

目标 AI 入口保持不变，但按调用角色分流：

```text
POST /api/ai-diagnosis/diagnose
POST /api/diagnosis/diagnose
```

```text
role = customer
-> 保持现有提示词 + 大模型初诊链路

role = technician/admin
-> 接入规则优先 + 多 Agent 辅助判断链路
```

### 1.2 前端

- Vue
- Axios
- CustomerDashboard AI 诊断页
- TechnicianDashboard AI Copilot
- AIDiagnosisClient 独立诊断页

前端当前已经支持：

- 文本输入
- 语音转文字输入
- 图片上传和预览
- 诊断结果展示
- 费用区间和工时区间展示

### 1.3 外部 AI 服务

沿用当前环境变量配置：

```properties
ai.diagnosis.api.key=${AI_DIAGNOSIS_API_KEY:}
ai.diagnosis.api.base-url=${AI_DIAGNOSIS_API_BASE_URL:https://api.apiyi.com/v1}
ai.diagnosis.api.model=${AI_DIAGNOSIS_API_MODEL:deepseek-chat}
```

外部服务必须兼容 OpenAI Chat Completions 协议。

## 2. 目标架构

目标架构不是替换全部诊断入口，而是在 `AIDiagnosisService` 内按 role 分流。

顾客端保持现有简化链路：

```text
AIDiagnosisController
-> AIDiagnosisService
-> CustomerPromptDiagnosis
-> AIDiagnosisResponse
```

技师端目标架构为“规则优先 + 多 Agent 辅助判断”：

```text
AIDiagnosisController
-> AIDiagnosisService
-> HybridDiagnosisEngineService
   -> PrivacyMasker
   -> RuleDiagnosisEngine
   -> SemanticAgent
   -> InventoryAgent
   -> HistoryCaseAgent
   -> DecisionFusionEngine
   -> SafetyNetGate
-> AIDiagnosisResponse
```

## 3. 核心组件职责

### 3.1 PrivacyMasker

职责：

- 在任何外部 LLM 请求前扫描敏感实体。
- 对车牌、VIN 等敏感内容做 mask。
- 记录占位符和原文映射。
- 默认不向外部模型发送原始敏感实体。

第一阶段支持：

- 车牌号：`[\u4e00-\u9fa5]{1}[A-Z]{1}[A-Z_0-9]{5}`
- 中国车牌扩展格式
- 17 位 VIN：排除 I、O、Q 的 VIN 规则

### 3.2 RuleDiagnosisEngine

职责：

- 对技师端高频明确故障做本地正则和决策树匹配。
- 输出故障类型、建议、严重等级、费用区间、工时区间、置信度、命中规则。
- 高置信结果允许直接返回，不调用外部 AI。

第一阶段规则库覆盖：

- 启动困难
- 制动异常
- 发动机异响/抖动/动力不足
- 变速箱顿挫/打滑
- 空调异常
- 电气/冒烟/焦糊味
- 冷却系统高温
- 油液泄漏

### 3.3 SemanticAgent

职责：

- 处理技师端规则低置信或复杂描述。
- 调用外部 LLM 做语义归一化和候选故障判断。
- 输出面向技师的专业排查证据。

约束：

- 输入必须使用脱敏后的文本。
- 输出优先要求 JSON。
- JSON 至少包含 `faultType`、`suggestion`、`possibleCauses`。
- 外部 AI 不直接决定工单状态。
- 不负责顾客端通俗化初诊，顾客端仍由现有提示词链路负责。

### 3.4 InventoryAgent

职责：

- 根据故障关键词查询现有材料和库存风险。
- 对可能涉及的零部件给出可用性提示。
- 为维修优先级和建议路径提供供给侧证据。

第一阶段数据源：

- `MaterialRepository`
- `InventoryAlertNotificationRepository`
- 关键词匹配材料名称。

第一阶段不做：

- 自动采购建议。
- 自动扣减库存。
- 自动改变维修优先级字段。

### 3.5 HistoryCaseAgent

职责：

- 根据故障关键词检索历史维修工单。
- 提供相似案例摘要、车型信息和历史处理路径。

第一阶段数据源：

- `RepairOrderRepository`
- `Vehicle`
- `RepairOrder.description`
- `RepairOrder.repairType`
- `RepairOrder.materialCost`
- `RepairOrder.laborCost`
- `RepairOrder.actualHours`

第一阶段实现方式：

- 关键词检索和简单排序。
- 不引入向量库。
- 不实现完整 RAG。

### 3.6 DecisionFusionEngine

职责：

- 融合规则结果和 Agent 证据。
- 生成最终故障类型、建议、可能原因、置信度、决策路径。
- 保留命中规则和 Agent 摘要，支持解释性展示。

第一阶段建议权重：

- 高置信规则直出：规则权重 100%。
- 规则 + Agent 融合：规则 45%，Agent 55%。
- 无有效规则时：规则 25%，Agent 75%。

权重可以先常量化，后续迁移到配置表。

### 3.7 SafetyNetGate

职责：

- 判断综合置信度是否低于阈值。
- 低于阈值时，在响应中标记“需人工复核”。
- 不自动创建人工复核任务，不修改工单状态。

第一阶段阈值：

```text
confidence < 0.85 -> SUSPENDED
confidence >= 0.85 -> VALIDATED
```

## 4. 响应数据格式

现有 `AIDiagnosisResponse` 字段：

```json
{
  "faultType": "制动系统故障",
  "suggestion": "检查制动液、刹车片、制动管路...",
  "severityLevel": "CRITICAL",
  "possibleCauses": ["制动液不足", "刹车片磨损"],
  "estimatedCostMin": 800,
  "estimatedCostMax": 3000,
  "estimatedHoursMin": 3,
  "estimatedHoursMax": 8,
  "success": true,
  "errorMessage": null
}
```

建议扩展字段：

```json
{
  "confidence": 0.91,
  "workflowStatus": "VALIDATED",
  "decisionPath": ["隐私脱敏", "规则命中: 刹车异常", "规则高置信直出"],
  "ruleHits": ["刹车异常"],
  "agentSummaries": []
}
```

兼容策略：

- 新字段只新增，不删除旧字段。
- 顾客端可忽略新增字段，保持原结果卡片展示。
- 技师端后续可增加“诊断证据链”展示。

## 5. Prompt 约束

外部 LLM Prompt 必须满足：

- 不包含原始车牌、VIN 等敏感实体。
- 明确 AI 是辅助诊断，不是最终检测结论。
- 要求输出 JSON，减少解析不稳定。
- 顾客端 Prompt 继续使用通俗语言和初步建议风格。
- 技师端 SemanticAgent Prompt 输出检测工具、数据点、排查顺序和高危操作提醒。
- 涉及制动、高温、燃油、高压电、电气短路时必须强化安全提示。

SemanticAgent 推荐 JSON 输出：

```json
{
  "coreSymptoms": ["冷车启动抖动", "故障灯偶发点亮"],
  "suspectedSystems": ["点火系统", "燃油供给", "进气系统"],
  "faultCandidates": ["火花塞老化", "点火线圈异常", "进气漏气"],
  "riskTags": ["可继续低速短途行驶但需尽快检测"],
  "confidence": 0.72
}
```

## 6. 部署和运行约束

- 不新增必须安装的基础设施组件。
- 不依赖 Redis、消息队列、向量数据库。
- 外部 AI Key 只能来自环境变量或本地 ignored 配置。
- 没有 API Key 或外部服务失败时，必须能返回本地规则诊断。
- 所有核心链路应可通过 Spring Boot 单元测试或 MockMvc 测试验证。

## 7. 测试策略

必须覆盖：

- 顾客端仍走现有提示词初诊链路。
- 技师端高频故障规则直出。
- 技师端复杂故障触发多 Agent。
- 技师端车牌/VIN 脱敏后再调用外部 AI。
- 技师端外部 AI 不可用时返回本地规则结果。
- 技师端低置信结果返回 `SUSPENDED`。
- 图片-only 输入保持兼容，不导致 500。
- 旧字段 `faultType`、`suggestion`、`severityLevel`、`estimatedHoursMin/Max` 继续返回。
