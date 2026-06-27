# 智慧车辆维修管理系统 - 未实现功能与特性分析报告

经过对《软件应用与开发类作品设计和开发文档.pdf》以及前后端项目源代码的详细交叉比对，目前系统中核心主干链路（包括用户与角色管理、智能派单、工单流转、基础 AI 文本诊断、绿色评估与零碳公路之旅游戏化等）已经实现。但与设计文档及演进规划相比，仍有以下几个**关键特性尚未完整实现或处于被遗弃（死代码）状态**：

## 1. 绿碳高级算法与评级映射 (死代码孤岛)（已实现）
* **文档描述**：系统通过多元线性回归算法校准碳排放影响系数，并将碳排放结果映射为 S/A/B/C 绿色指数等级。
* **代码现状**：代码库中确实存在完整的 `GreenEmissionEngine` 和 `GreenIndexGrade` 枚举类，但经过全局追踪，**这些类完全没有被注册到 Spring 容器中，也从未在任何 Service 中被调用。** 系统目前实际使用的只是 `EmissionCalculatorService` 中一个极简的硬编码乘法公式，并没有真正启用高阶的绿碳算法与评级映射功能。

## 2. 基于 Aging 的反饥饿调度策略 (死代码孤岛)
* **文档描述**：为防止复杂长尾工单长时间未被接单，设计了基于时间的饥饿递增策略，等待时间越长优先级越高。
* **代码现状**：代码库中存在对应的策略类 `AgingAntiStarvationDispatchPolicy`，其包含了详尽的等待惩罚和加权算法。然而与绿碳引擎类似，它在当前主流程的 `AutoAssignmentService` 中**完全没有被引入或调用**，属于未接入主控链路的废弃状态。

## 2.5. 疲劳度惩罚 — 派单评分层面存在关键断点 (部分实现)

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

### 未实现的关键断点

#### d. 疲劳度 → 派单评分惩罚：数据流完全断裂

设计文档描述的”疲劳度惩罚”应影响技师派单评分，但当前存在**数据流断点**：

```
FeedbackSelfIterationService  →  计算 fatiguePenaltyWeight  →  写入 dispatch_weight_config 表
                                                                         ↓
                                                                    ❌ 无消费者
                                                                         ↓
AutoAssignmentService.calculateTechnicianScore()  →  使用硬编码权重，与疲劳度完全无关
```

具体问题：

1. **`AutoAssignmentService.calculateTechnicianScore()`**（[第 149-163 行](../backend/src/main/java/org/com/repair/service/AutoAssignmentService.java#L149-L163)）使用硬编码权重 `0.5/0.3/0.2`，其中 `workloadScore` 仅仅是 `max(0, 10 - 活跃工单数)`——这是一个简单的工作负载计数，**完全不是疲劳度**。
2. 该方法**没有注入 `TechnicianService` 依赖**，因此无法调用 `getTechnicianFatigueSnapshot()` 获取真实疲劳度数据。
3. **没有读取 `DispatchWeightConfig` 表**（虽然 `DispatchWeightConfigRepository` 存在但无消费者），`fatiguePenaltyWeight` 字段无人问津。
4. `FeedbackSelfIterationService.proposeDispatchWeights()`（[第 115-127 行](../backend/src/main/java/org/com/repair/service/FeedbackSelfIterationService.java#L115-L127)）能根据低分反馈率计算并生成 `fatiguePenaltyWeight` 的 UPDATE SQL，但同样因缺少触发入口和审批界面而无法形成闭环（详见第 5 节）。

**结论**：`DispatchWeightConfig.fatiguePenaltyWeight` 是一条”断头路”——数据库表存在、实体字段存在、自迭代能写入，但派单评分的消费者端完全不走这条路。之前报告中”已通过扣除技师工作负载得分的方式正确实现”的说法不准确，工作负载计数 ≠ 疲劳度惩罚。

### 修复建议

要使疲劳度惩罚真正生效，至少需要：
1. 在 `AutoAssignmentService` 中注入 `TechnicianService`，调用 `getTechnicianFatigueSnapshot()` 获取疲劳度
2. 在 `calculateTechnicianScore()` 中引入疲劳惩罚项：`score -= fatigueLevel * fatiguePenaltyWeight`
3. 从 `DispatchWeightConfig` 表动态读取权重值（替代硬编码），或至少让 `fatiguePenaltyWeight` 参与计算

## 3. 多模态图像识别能力 (FlexVAI 视觉检测)(已实现)
* **文档描述**：文档中多次提及“深度集成大语言模型与计算机视觉技术”，“FlexVAI mAP@50 达到 0.986，进行宏观渗漏检测和微观零件分析”。
* **代码现状**：在先前的修改中，已经将外部模型切换为支持多模态的小米 mimo v2.5，重构了 `AIDiagnosisService`，使其在用户上传图片时（包含或不包含文本），均会将图片 URL 和文本一并构建为多模态负载发给大模型，真正利用了大模型的 Vision 视觉能力。

## 4. 混合诊断中“多 Agent 协同推理”流程并未接入主控链路
* **文档描述**：设计了包括“主治工程师”、“红队QA”和“总控车间主任（Arbitrator）”的多阶段协同问诊机制。
* **代码现状**：在 `AIDiagnosisService.java` 中，虽然存在完整的 `expertConsilium` 方法以及相关的 Prompt 定义（红队、主治、裁判），但该方法被标注了 `@SuppressWarnings("unused")` 并且在主流程 `diagnoseFault` 中**完全没有被调用**。目前线上的主流程仅仅是调用 `externalSingleDiagnosis` 发起单次 LLM 请求。

## 5. 算法自演进闭环机制缺少执行与审批端点
* **文档描述**：基于业务数据反馈的算法自演进闭环，"零算力损耗的免重训自演进架构"，系统能够自动生成 Prompt 补丁并调整派单权重。
* **代码现状**：后端 `FeedbackSelfIterationService` 确实实现了数据统计并生成了调整权重的建议及 `UPDATE` SQL（`buildConfigUpsertPreviewSql`），生成了 `SelfIterationDraft` 对象。但是，**系统缺失触发该逻辑的 API 入口、定时任务以及管理员前端的审核/执行界面**，导致这套机制无法真正在业务中形成闭环。
