# AI 诊断 Agent 架构文档

## 项目背景

本项目源自中国大学生计算机设计大赛参赛作品**"方行唯'AI'——面向全国300万中小维修门店的智慧车辆维修管理系统"**（作品编号：2026091181，版本 v5）。

项目旨在为中小维修门店提供智能化车辆维修管理解决方案，解决行业四大痛点：
- 信息化程度有限，传统系统依赖人工填写
- 智能化能力不足，纯 AI 方案缺乏专业领域约束且结果不稳定
- 资源调度低效，工单分配依赖人工经验
- 绿色化管控缺失，无法自动归集环保数据

**注意**: 本文档仅覆盖 AI 诊断模块的架构设计。完整项目设计请参阅 `软件应用与开发类作品设计和开发文档.pdf`。

## 概述

系统采用**混合诊断引擎**架构，融合规则引擎、多 Agent 协同推理和外部 LLM，为客户提供故障预判，为技师提供维修辅助建议。

核心服务类：`AIDiagnosisService`（编排入口）、`RuleDiagnosisService`、`SemanticDiagnosisAgent`、`InventoryDiagnosisAgent`、`HistoryCaseAgent`、`DecisionFusionEngine`、`SafetyNetGate`、`PrivacyMaskingService`

## 诊断流程

```
用户输入（文字/图片/语音）
       │
       ▼
┌─────────────────┐
│ PrivacyMaskingService │ ← VIN、车牌号脱敏
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ RuleDiagnosisService │ ← 7 条正则规则，毫秒级匹配高频故障
└────────┬────────┘
         │
    ┌────┴────┐
    │ 规则命中? │
    └────┬────┘
    Yes  │  No
    │    │
    │    ▼
    │  ┌──────────────────┐
    │  │ 技师端多 Agent 协同推理 │
    │  ├──────────────────┤
    │  │ PRE_DIAG          │ ← 结构化故障特征提取，支持图片/语音证据
    │  │ MAIN_AGENT        │ ← 主治维修工程师初诊
    │  │ RED_TEAM          │ ← 红队 QA 质疑长尾/高危漏判
    │  │ ARBITRATOR        │ ← 总控车间主任融合裁决
    │  │ InventoryDiagnosisAgent │ ← 库存匹配 + 低库存预警
    │  │ HistoryCaseAgent  │ ← 历史工单相似案例检索
    │  └────────┬─────────┘
    │           │
    │           ▼
    │  ┌──────────────────┐
    │  │ DecisionFusionEngine │ ← 加权融合规则信号 + Agent 证据
    │  └────────┬─────────┘
    │           │
    │           ▼
    │  ┌──────────────────┐
    │  │  SafetyNetGate    │ ← 置信度 >= 0.85 → VALIDATED
    │  └────────┬─────────┘     置信度 <  0.85 → SUSPENDED
    │           │
    └─────┬─────┘
          │
          ▼
    诊断结果返回（含置信度、决策路径、Agent 摘要）
```

## 各组件详解

### 1. RuleDiagnosisService — 规则预诊断

**职责**: 基于 7 条预定义正则规则快速匹配常见故障，覆盖大部分基础场景。

**规则列表**:
- 制动系统故障（刹车异响、制动失灵等）
- 发动机故障（抖动、过热、异响等）
- 电气系统故障（灯光、电瓶、启动等）
- 冷却系统故障（水温高、漏液等）
- 启动故障（无法启动、启动困难等）
- 空调故障（不制冷、异味等）
- 漏油故障（机油、变速箱油等）

**返回**: `RuleDiagnosisResult`（matched, faultType, confidence, suggestion, directReturn）

### 2. SemanticDiagnosisAgent — 语义诊断 Agent

**职责**: 调用外部 LLM API 进行深度语义分析，处理规则引擎无法覆盖的复杂场景。

**实现**: 通过函数式接口（`PromptComposer`、`ExternalAiCaller`、`ResponseParser`）解耦，便于测试和替换 LLM 提供商。

**角色区分**:
- 客户角色：通过 `externalSingleDiagnosis()` 返回通俗易懂的故障描述和建议
- 技师/admin 角色：非高置信规则直出场景进入 `expertConsilium()`，在一次外部 LLM 请求内完成四阶段协同会诊（`PRE_DIAG → MAIN_AGENT → RED_TEAM → ARBITRATOR`），附加决策路径说明

### 2.5. ExpertConsilium — 技师端四阶段协同会诊

**职责**: 将设计文档中的“主治工程师 / 红队 QA / 总控车间主任”协同问诊接入 `AIDiagnosisService.diagnoseFault()` 主控链路。为避免技师工作台交互超时，四阶段协作在单次 `AI_CONSILIUM` 外部请求中完成，而不是 4 次串行网络调用。

**阶段**:
- `PRE_DIAG`：基于脱敏后的用户描述提取结构化故障特征；如请求包含图片或语音，则在该阶段一并提取多模态线索。
- `MAIN_AGENT`：主治维修工程师基于结构化特征、库存证据和历史案例证据输出初诊路径。
- `RED_TEAM`：红队 QA 审视初诊结论，补充低概率高风险故障和逻辑漏洞。
- `ARBITRATOR`：总控车间主任融合主治诊断、红队质疑、库存/历史证据和技师疲劳度，生成最终报告。

**安全规则**: 当技师疲劳度 `> 0.7` 时，`ARBITRATOR` 必须拆解高危步骤，并在高压电、燃油等操作前插入 `[安全高危校验]`。

### 3. InventoryDiagnosisAgent — 库存诊断 Agent

**职责**: 将问题描述中的关键词匹配到库存物料，检查可用性和低库存预警。

**输出**: 库存证据摘要（匹配的物料列表、库存状态、安全库存警告）

### 4. HistoryCaseAgent — 历史案例 Agent

**职责**: 在已完成的维修工单中检索相似案例，基于关键词匹配和评分排序。

**输出**: 历史案例证据摘要（相似案例列表、维修方案、成功率）

**隐私**: 通过 `PrivacyMaskingService` 对历史案例中的敏感信息进行脱敏。

### 5. DecisionFusionEngine — 决策融合引擎

**职责**: 将规则引擎信号和多 Agent 证据进行加权融合，计算综合置信度。

**融合策略**:
- 规则命中提供基础置信度
- Agent 证据按权重叠加
- 冲突信号进行消解

**输出**: 融合后的置信度分数 + 工作流状态（VALIDATED / SUSPENDED）

### 6. SafetyNetGate — 安全网关

**职责**: 置信度阈值门控，确保高置信度结果自动输出，低置信度进入人工复核。

**阈值**: 0.85

**状态**:
- `VALIDATED` — 置信度 >= 0.85，自动输出诊断报告
- `SUSPENDED` — 置信度 < 0.85，需人工复核

### 7. PrivacyMaskingService — 隐私脱敏

**职责**: 使用正则表达式识别并脱敏文本中的 VIN 码和中国车牌号。

**策略**: 本地正则扫描，无损脱敏（保留前后字符，中间替换为 `***`）

## 自迭代机制

系统支持基于用户反馈的自动优化，无需重新训练模型。

### FeedbackSelfIterationService

- **输入**: 最近 N 天的用户反馈评分
- **分析**: 低分反馈占比、高频问题类型
- **输出**:
  - 派单权重调整建议（更新 `dispatch_weight_config` 表）
  - Prompt 模板补丁建议（更新 `agent_prompt_template_config` 表）
  - 预览 SQL（供管理员审核）
- **触发方式**:
  - 每日 03:15 通过 `@Scheduled` 自动生成最新待审草案，不直接修改线上配置
  - 管理员 API `POST /api/admins/ai-self-iteration/draft` 可手动生成草案
  - 管理员 API `POST /api/admins/ai-self-iteration/approve` 审批后通过 JPA 写入 `agent_prompt_template_config` 与 `dispatch_weight_config`
- **审核界面**: 管理员控制台 `AI 自演进` 页面展示反馈聚合、Prompt 补丁、派单权重、SQL 预览，并支持审批落库

### 配置表

| 表 | 用途 |
|---|------|
| `agent_prompt_template_config` | Agent Prompt 模板，按角色和模板键存储 |
| `dispatch_weight_config` | 派单权重参数（评分权重、工作量权重、经验权重、疲劳惩罚） |

## 角色交互差异

| 特性 | 客户端 | 技师端 |
|------|--------|--------|
| 输入方式 | 文字 + 图片 + 语音 | 文字 + 图片 |
| 结果深度 | 通俗描述 + 估算费用/工时 | 技术诊断 + 决策路径 + 规则命中详情 |
| 后续操作 | 可一键转为维修工单 | 可用于辅助判断，结合 AI Copilot 面板 |
| 隐私策略 | 全量脱敏 | 全量脱敏 |

## 相关文件

| 文件 | 说明 |
|------|------|
| `backend/src/main/java/org/com/repair/service/AIDiagnosisService.java` | 诊断编排主服务 |
| `backend/src/main/java/org/com/repair/service/RuleDiagnosisService.java` | 规则诊断 |
| `backend/src/main/java/org/com/repair/service/SemanticDiagnosisAgent.java` | 语义 Agent |
| `backend/src/main/java/org/com/repair/service/InventoryDiagnosisAgent.java` | 库存 Agent |
| `backend/src/main/java/org/com/repair/service/HistoryCaseAgent.java` | 历史案例 Agent |
| `backend/src/main/java/org/com/repair/service/DecisionFusionEngine.java` | 决策融合 |
| `backend/src/main/java/org/com/repair/service/SafetyNetGate.java` | 安全网关 |
| `backend/src/main/java/org/com/repair/service/PrivacyMaskingService.java` | 隐私脱敏 |
| `backend/src/main/java/org/com/repair/service/FeedbackSelfIterationService.java` | 反馈自迭代 |
| `backend/src/main/java/org/com/repair/controller/AIDiagnosisController.java` | REST API 入口 |
| `backend/src/main/java/org/com/repair/architecture/agent/HybridDiagnosisEngine.java` | 独立展示用引擎（非 Spring 管理） |
| `docs/ai-diagnosis/missing_features_report.md` | 缺失功能清单与待完善项 |

## 文档与质量纪律

1. **文档同步更新**: 每次修改代码或文件后，必须检查 `CLAUDE.md` 和 `agents.md` 是否需要同步更新（如适用），并在必要时立即更新。包括但不限于：新增/删除文件或目录、变更 Agent 架构或组件、调整诊断流程。
2. **代码验证**: 每次修改代码之后，必须进行检查与测试（如适用，运行相关单元测试或 E2E 测试），确保代码没有问题。
3. **文档二次复核**: 每次修改 `agents.md` 或 `CLAUDE.md` 之后，必须进行二次复核，确认内容与代码实际状态一致、无遗漏或错误。
