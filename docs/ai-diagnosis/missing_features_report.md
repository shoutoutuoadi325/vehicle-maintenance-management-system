# 智慧车辆维修管理系统 - 未实现功能与特性分析报告

经过对《软件应用与开发类作品设计和开发文档.pdf》以及前后端项目源代码的详细交叉比对，目前系统中核心主干链路（包括用户与角色管理、智能派单、工单流转、基础 AI 文本诊断、绿色评估与零碳公路之旅游戏化等）已经实现。但与设计文档及演进规划相比，仍有以下几个**关键特性尚未完整实现或处于被遗弃（死代码）状态**：

## 1. 绿碳高级算法与评级映射 (死代码孤岛)（已实现）
* **文档描述**：系统通过多元线性回归算法校准碳排放影响系数，并将碳排放结果映射为 S/A/B/C 绿色指数等级。
* **代码现状**：代码库中确实存在完整的 `GreenEmissionEngine` 和 `GreenIndexGrade` 枚举类，但经过全局追踪，**这些类完全没有被注册到 Spring 容器中，也从未在任何 Service 中被调用。** 系统目前实际使用的只是 `EmissionCalculatorService` 中一个极简的硬编码乘法公式，并没有真正启用高阶的绿碳算法与评级映射功能。

## 2. 基于 Aging 的反饥饿调度策略（已实现）
* **文档描述**：为防止复杂长尾工单长时间未被接单，设计了基于时间的饥饿递增策略，等待时间越长优先级越高。
* **代码现状**：`AgingAntiStarvationDispatchPolicy` 已注册为 Spring 组件，并接入 `AutoAssignmentService` 主流程：
  - `AutoAssignmentService.autoAssignBestTechnician(RepairOrder)` 使用工单上下文调用 `calculateDispatchScore()`，将等待时长、长尾工单识别和技能匹配加成纳入技师派单评分。
  - `AutoAssignmentService.rankPendingOrdersByAging()` 对待分配工单按 Aging 优先级排序。
  - `RepairOrderService.reassignPendingOrders()` 已改为先按 Aging 队列处理积压工单，再执行自动派单，避免长时间等待的复杂工单继续被排在普通新单之后。
* **测试覆盖**：`AutoAssignmentServiceTest.shouldRankOlderLongTailPendingOrdersFirstByAgingPolicy()` 验证等待更久且具备长尾特征的工单会排在普通新单之前。

## 2.5. 疲劳度惩罚 — 派单评分层面关键断点（已实现）

### 已真实实现的部分

#### a. 疲劳度计算算法（`TechnicianService.getTechnicianFatigueSnapshot()`）
基于三个维度计算技师疲劳度（归一化至 [0, 1]），算法真实可行：

| 维度 | 上限 | 权重 | 计算逻辑 |
|------|------|------|----------|
| `completedFactor` | 今日完成 ≤8 单 | 0.40 | 今日实际完成工单数 / 8 |
| `continuousFactor` | 连续工作 ≤6 小时 | 0.35 | 含区间合并算法：将今日各工单时间窗口排序，间隔 ≤30 分钟视为连续，取最长连续段 |
| `nightFactor` | 夜间工单 ≤3 单 | 0.15 | 22:00–06:00 间的工单数 / 3 |
| `BASELINE_FATIGUE` | — | 0.10 | 基础疲劳度常数 |

最终公式：`fatigueLevel = 0.1 + completedFactor×0.4 + continuousFactor×0.35 + nightFactor×0.15`，结果归一化至 [0, 1]。

- **代码位置**：[`TechnicianService.java:216-252`](../backend/src/main/java/org/com/repair/service/TechnicianService.java#L216-L252)
- **测试覆盖**：`TechnicianServiceFatigueTest` 包含 3 个测试用例（基线疲劳、正常计算、极端上限）
- **评价**：✅ 非敷衍，有真实算法实现与单元测试

#### b. AI 诊断提示中的疲劳度注入（`AIDiagnosisService.buildOperationalContext()`）
在主流程 `diagnoseFault() → externalSingleDiagnosis()` 中，`buildOperationalContext()` **确实被调用**，将疲劳度写入每次 LLM 请求的 Prompt 上下文：
- 注入 `当前技师疲劳度：{fatigueLevel}（0到1之间）`
- 当 `fatigueLevel > 0.7` 时，追加安全提示：*”若涉及高压电、燃油、制动或高温部件，请把安全校验步骤写得更明确”*
- **代码位置**：[`AIDiagnosisService.java:195-213`](../backend/src/main/java/org/com/repair/service/AIDiagnosisService.java#L195-L213)
- **评价**：✅ 真实生效，疲劳度数据影响 LLM 安全输出

### 有壳子但未接入主链路的部分

#### c. 多 Agent 协同中的疲劳度深度集成（`expertConsilium()`）
- **代码位置**：[`AIDiagnosisService.java:216-251`](../backend/src/main/java/org/com/repair/service/AIDiagnosisService.java#L216-L251)
- **现状**：该方法在 ARBITRATOR 阶段设计了更细粒度的疲劳度动态安全规则（>0.7 时强制”傻瓜式”拆解 + `[安全高危校验]` 物理确认步骤），但被标注 `@SuppressWarnings(“unused”)`，**在主流程中完全没有被调用**
- **评价**：⚠️ 死代码，详见第 4 节”多 Agent 协同推理”

### 已修复的关键断点

#### d. 疲劳度 → 派单评分惩罚：数据流已打通

设计文档描述的”疲劳度惩罚”现在已经影响技师派单评分：

```
FeedbackSelfIterationService  →  计算 fatiguePenaltyWeight  →  写入 dispatch_weight_config 表
                                                                         ↓
                                                           AutoAssignmentService 读取 default 配置
                                                                         ↓
AutoAssignmentService.calculateTechnicianScore()  →  score -= fatigueLevel * fatiguePenaltyWeight
```

具体实现：

1. **`AutoAssignmentService.calculateTechnicianScore()`** 不再使用固定写死的 `0.5/0.3/0.2`，而是通过 `DispatchWeightConfigRepository.findByConfigKeyAndEnabledTrue("default")` 读取评分权重；配置缺失时保留旧权重作为兜底。
2. **`AutoAssignmentService` 已注入 `TechnicianService`**，通过 `getTechnicianFatigueSnapshot()` 获取真实疲劳度数据。
3. **`fatiguePenaltyWeight` 已成为评分消费者字段**：最终评分公式为 `ratingScore×ratingWeight + workloadScore×workloadWeight + experienceScore×experienceWeight - fatigueLevel×fatiguePenaltyWeight`。
4. **工单上下文已参与派单**：`RepairOrderService.createRepairOrder()`、`RepairOrderService.reassignPendingOrders()` 和技师拒单后的重分配路径均优先调用带 `RepairOrder` 上下文的自动派单入口。

**测试覆盖**：`AutoAssignmentServiceTest.shouldApplyConfiguredFatiguePenaltyWhenSelectingTechnician()` 验证当 `dispatch_weight_config.fatigue_penalty_weight` 生效时，高疲劳技师即使评分略高也会被惩罚，低疲劳技师会被优先分配。

## 3. 多模态图像识别能力 (FlexVAI 视觉检测)(已实现)
* **文档描述**：文档中多次提及“深度集成大语言模型与计算机视觉技术”，“FlexVAI mAP@50 达到 0.986，进行宏观渗漏检测和微观零件分析”。
* **代码现状**：在先前的修改中，已经将外部模型切换为支持多模态的小米 mimo v2.5，重构了 `AIDiagnosisService`，使其在用户上传图片时（包含或不包含文本），均会将图片 URL 和文本一并构建为多模态负载发给大模型，真正利用了大模型的 Vision 视觉能力。

## 4. 混合诊断中“多 Agent 协同推理”流程并未接入主控链路（已实现）
* **文档描述**：设计了包括“主治工程师”、“红队QA”和“总控车间主任（Arbitrator）”的多阶段协同问诊机制。
* **代码现状**：`AIDiagnosisService.diagnoseFault()` 已将技师端/admin 端非规则直出路径接入 `expertConsilium()`，在一次外部 LLM 请求内按 `PRE_DIAG → MAIN_AGENT → RED_TEAM → ARBITRATOR` 完成协同推理，避免 4 次串行网络调用导致前端交互超时。车主端仍保留 `externalSingleDiagnosis()` 单次语义诊断，避免向普通用户暴露过深的专业会诊链路。
* **已接入上下文**：
  - `PRE_DIAG` 阶段使用脱敏后的用户描述，并支持图片/语音多模态证据输入。
  - `MAIN_AGENT` 与 `ARBITRATOR` 阶段注入 `InventoryDiagnosisAgent` 库存证据和 `HistoryCaseAgent` 历史案例证据。
  - `ARBITRATOR` 阶段继续消费技师疲劳度上下文，疲劳度高于 0.7 时要求插入更细粒度安全校验步骤。
  - `DecisionFusionEngine` 已能在 Agent 摘要中区分 `AI Consilium: multi-stage diagnosis parsed` 与单次 `Semantic Agent` 诊断。
* **测试覆盖**：`AIDiagnosisServiceFusionTest.technicianLowConfidenceSemanticResultShouldBeSuspended()` 验证技师端低置信场景会通过 `AI_CONSILIUM` 单次外部调用执行，Prompt 内包含 `PRE_DIAG → MAIN_AGENT → RED_TEAM → ARBITRATOR` 四阶段协同要求，并进入 SafetyNet 人工复核分支；隐私、库存、历史案例测试同步覆盖四阶段链路中的脱敏与上下文注入。

## 5. 算法自演进闭环机制缺少执行与审批端点（已实现）
* **文档描述**：基于业务数据反馈的算法自演进闭环，"零算力损耗的免重训自演进架构"，系统能够自动生成 Prompt 补丁并调整派单权重。
* **代码现状**：闭环已接入执行与审批链路：
  - `FeedbackSelfIterationService` 保留反馈聚合、Prompt 补丁、派单权重建议和 SQL 预览，并新增每日 03:15 `@Scheduled` 自动生成最新待审草案。
  - 管理员 API 已接入：`GET /api/admins/ai-self-iteration/draft` 查看待审草案，`POST /api/admins/ai-self-iteration/draft` 手动生成草案，`POST /api/admins/ai-self-iteration/approve` 审批并通过 JPA 写入 `agent_prompt_template_config` 与 `dispatch_weight_config`。
  - 管理员控制台新增 `AI 自演进` 页面，支持查看反馈聚合、Prompt 补丁、派单权重建议、SQL 预览，并填写审批备注后写入配置。
* **测试覆盖**：`FeedbackSelfIterationServiceTest` 覆盖草案生成与审批落库；`AdminControllerResilienceTest.shouldGenerateSelfIterationDraftFromAdminEndpoint()` 覆盖管理员端点触发草案生成。
