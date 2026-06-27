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
    │  │ 多 Agent 并行推理  │
    │  ├──────────────────┤
    │  │ SemanticDiagnosisAgent │ ← 外部 LLM API（小米mimo v2.5）
    │  │ InventoryDiagnosisAgent │ ← 库存匹配 + 低库存预警
    │  │ HistoryCaseAgent │ ← 历史工单相似案例检索
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
- 客户角色：返回通俗易懂的故障描述和建议
- 技师角色：返回技术性诊断，附加决策路径说明

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
  - 预览 SQL（人工确认后执行）

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
| `docs/architecture/agent-architecture.md` | 架构流程图（Mermaid） |
| `docs/ai-diagnosis/mission.md` | 需求文档 |
| `docs/ai-diagnosis/tech-stack.md` | 技术架构 |
| `docs/ai-diagnosis/roadmap.md` | 实施路线 |
