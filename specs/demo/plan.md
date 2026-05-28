# 短期 Demo 开发计划

## 1. 当前阶段判断

根据 `specs/roadmap.md`，阶段 0“规格冻结”已经完成，当前应该推进的下一个开发阶段是阶段 1“技师端规则优先接入”。

本目录将该阶段扩展为短期 Demo 交付计划：先完成技师端规则优先的最小闭环，再逐步接入隐私脱敏、Semantic Agent、Inventory Agent、History Case Agent、决策融合和兼容验证。顾客端链路保持现有大模型提示词初诊模式，不在本阶段接入规则 + 多 Agent 证据链。

## 2. 开发原则

- 每个任务组必须能独立验证、独立回滚、独立合并。
- 优先保证现有 `/api/ai-diagnosis/diagnose` 和 `/api/diagnosis/diagnose` 可用。
- 先接入技师端链路，顾客端只做兼容验证。
- 规则高置信允许直出，不调用外部 AI。
- 规则低置信或复杂描述再触发 Agent 辅助。
- 不引入 Redis、消息队列、向量数据库或异步编排。
- 不让 AI 自动创建、关闭、改派、结算工单或扣减库存。

## 3. 任务组 1：技师端规则优先最小闭环

目标：把技师端从“外部 AI 优先，规则兜底”改为“本地规则优先，高置信直出”。

主要任务：

1. 抽取或复用现有 `HybridDiagnosisEngine` 中的规则预处理逻辑。
2. 建立正式 Spring 服务，例如 `RuleDiagnosisService` 或 `RuleDiagnosisEngine`。
3. 在 `AIDiagnosisService.diagnoseFault` 中按 `role` 分流。
4. `customer` 继续走现有提示词初诊链路。
5. `technician/admin` 先进入规则优先链路。
6. 将规则结果映射为 `AIDiagnosisResponse` 旧字段。
7. 增加规则命中测试。

建议覆盖规则：

- 制动异常：刹车变软、刹车异响、刹车距离变长。
- 发动机异常：发动机抖动、故障灯亮、动力不足。
- 电气和高温风险：冒烟、焦糊味、水温高。
- 启动困难、空调异常、油液泄漏可作为扩展规则。

可合并条件：

- 技师端高频故障能本地规则直出。
- 顾客端同类输入仍走原链路。
- 旧响应字段保持兼容。

## 4. 任务组 2：隐私脱敏接入

目标：在技师端外部 AI 请求前完成车牌和 VIN mask。

主要任务：

1. 建立 `PrivacyMasker` 服务。
2. 支持中国车牌和 17 位 VIN 识别。
3. 在技师端 Agent 调用外部 AI 前使用脱敏文本。
4. 在 `decisionPath` 中记录脱敏步骤。
5. 增加单元测试或 Mock 测试，确认外部 AI Prompt 不含原始敏感文本。

可合并条件：

- 输入包含车牌或 VIN 时，出站 Prompt 只包含 mask 后文本。
- 脱敏不影响最终诊断正常返回。
- 不把原始敏感实体重新暴露到 Agent 摘要中。

## 5. 任务组 3：Semantic Agent 接入

目标：规则低置信或未命中时，调用外部 LLM 生成技师可复核的语义诊断证据。

主要任务：

1. 将当前 `externalSingleDiagnosis` 能力封装为 `SemanticAgent`。
2. Prompt 面向技师端，要求 JSON 输出。
3. Prompt 注入脱敏后的症状文本、绿色维修提示和必要安全约束。
4. 解析 JSON 输出为结构化证据。
5. 对 Markdown 包裹 JSON、非标准 JSON、外部 AI 失败提供兜底。

可合并条件：

- 复杂描述能触发外部 AI。
- JSON 输出能稳定解析。
- 外部 AI 不可用时不影响本地规则可用性。
- 顾客端输出不受影响。

## 6. 任务组 4：Inventory Agent 接入

目标：让技师端诊断参考现有材料和库存预警，不改变库存状态。

主要任务：

1. 建立 `InventoryDiagnosisAgent`。
2. 根据故障关键词推断可能材料名称。
3. 查询 `MaterialRepository` 和 `InventoryAlertNotificationRepository`。
4. 输出库存摘要和风险标签。
5. 增加库存 Agent 测试。

可合并条件：

- 输入“刹车异响”时能关联刹车片、制动液等材料。
- 低库存材料能出现在 Agent 摘要中。
- 不自动扣减库存，不自动创建采购任务。

## 7. 任务组 5：History Case Agent 接入

目标：利用历史维修工单为复杂故障提供相似案例摘要。

主要任务：

1. 建立 `HistoryCaseAgent`。
2. 基于关键词检索 `RepairOrder`。
3. 关联车辆品牌、车型、年份等非隐私信息。
4. 限制返回数量，输出相似案例摘要。
5. 过滤用户隐私字段。

可合并条件：

- 输入“发动机抖动”时能返回相似历史工单摘要。
- 无相似案例时返回明确空摘要。
- 摘要不包含用户隐私。

## 8. 任务组 6：决策融合与人工复核标记

目标：把规则结果、Semantic Agent、Inventory Agent 和 History Case Agent 结果融合为最终结构化响应。

主要任务：

1. 建立 `DecisionFusionEngine`。
2. 建立 `SafetyNetGate`。
3. 扩展 `AIDiagnosisResponse`，新增 `confidence`、`workflowStatus`、`decisionPath`、`ruleHits`、`agentSummaries`。
4. 高置信规则直出时设置 `workflowStatus = VALIDATED`。
5. 低置信结果设置 `workflowStatus = SUSPENDED`。
6. 确保低置信只标记人工复核，不改变工单状态。

可合并条件：

- 技师端响应包含旧字段和新增证据字段。
- 高置信场景返回 `VALIDATED`。
- 低置信场景返回 `SUSPENDED`。
- 不发生任何自动工单状态写入。

## 9. 任务组 7：接口和前端兼容验证

目标：确认短期 Demo 不破坏现有前端入口和接口调用。

主要任务：

1. 跑通 `CustomerDashboard` AI 诊断。
2. 跑通 `TechnicianDashboard` AI Copilot。
3. 跑通 `AIDiagnosisClient` 独立诊断页。
4. 验证图片-only 输入不返回 500。
5. 验证旧字段仍可被前端结果卡片展示。
6. 如果技师端暂不展示新增字段，至少保留接口响应和调试证据。

可合并条件：

- 三个前端入口均能提交诊断。
- 顾客端不展示 Agent 内部证据链。
- 技师端可通过接口响应看到规则命中、Agent 摘要和决策路径。

## 10. 任务组 8：Demo 文档和答辩口径同步

目标：统一 README、架构说明和答辩 Demo 脚本，避免宣传超过实现范围。

主要任务：

1. 更新 README 中的 AI 维修模块说明。
2. 更新 Agent 架构说明，区分已接入能力和后续增强能力。
3. 准备 Demo 操作脚本。
4. 标明不承诺真实 RAG、并发 Agent、真实多模态视觉诊断。

可合并条件：

- 文档不再把已接入能力描述成纯展示代码。
- 文档没有承诺尚未实现的能力。
- Demo 脚本能稳定复现规则直出和 Agent 辅助判断。
