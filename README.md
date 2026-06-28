# 方行唯"AI"——面向全国 300 万中小维修门店的智慧车辆维修管理系统

# FangXingWei "AI" — Intelligent Vehicle Maintenance Management System for 3 Million Small and Medium Repair Shops Nationwide

## 一、项目概述
## 1. Project Overview

**方行唯 AI 智能汽车维修系统**是一款面向全国 300 万中小维修门店的智慧车辆维修管理平台，覆盖客户、技师、管理员三种角色，提供从多模态智能报修、混合 AI 故障诊断、疲劳感知动态派单、绿色碳排放评估到"零碳公路之旅"游戏化运营的全链路闭环解决方案。

**FangXingWei AI** is an intelligent vehicle maintenance management platform designed for 3 million small and medium repair shops across China. Covering three roles — customer, technician, and administrator — it provides a full-chain closed-loop solution spanning multimodal intelligent repair reporting, hybrid AI fault diagnosis, fatigue-aware dynamic dispatch, green carbon emission assessment, and the "Zero-Carbon Road Trip" gamification operation.

截至 2025 年底，全国汽车保有量达 3.66 亿辆，平均车龄 7.5 年，进入高频维修期；新能源汽车保有量超 4400 万辆，三电系统维修需求激增。在"双碳"目标与产业升级背景下，绿色维修指标纳入全国统一标准，广大维修企业亟需数字化、绿色化低碳转型。

By the end of 2025, China's automobile ownership reached 366 million units with an average vehicle age of 7.5 years, entering a high-frequency maintenance period. New energy vehicle ownership exceeded 44 million units, driving surging demand for three-electric-system (battery/motor/electronic control) repairs. Against the backdrop of "dual carbon" goals and industrial upgrading, green maintenance standards have been incorporated into national unified benchmarks, making digital and green low-carbon transformation an urgent imperative for repair enterprises nationwide.

## 二、行业痛点
## 2. Industry Pain Points

团队线下走访多家维修门店后发现，现有维修管理系统存在四大痛点：
Through on-site visits to numerous repair shops, our team identified four major pain points in existing maintenance management systems:

| 痛点<br>Pain Point | 现状<br>Current State | 本项目对策<br>Our Solution |
|------|------|------------|
| **信息化程度有限<br>Limited Digitization** | 传统系统依赖人工填写，难以适配口语化、多模态报修需求<br>Traditional systems rely on manual input; cannot accommodate colloquial or multimodal repair reporting | 多模态智能报修（文字/语音/图片）<br>Multimodal intelligent reporting (text/voice/image) |
| **智能化能力不足<br>Insufficient Intelligence** | 纯 AI 方案缺乏专业领域约束且结果不稳定，纯规则系统难以处理复杂模糊场景<br>Pure AI lacks domain constraints and yields unstable results; pure rule systems struggle with complex, ambiguous scenarios | "规则优先 + 多 Agent 兜底"混合智能诊断<br>"Rules-first + Multi-Agent fallback" hybrid intelligent diagnosis |
| **资源调度低效<br>Inefficient Resource Scheduling** | 工单分配依赖人工经验，未考虑技师疲劳度与技能匹配<br>Order assignment relies on manual experience, ignoring technician fatigue and skill matching | 运筹学疲劳感知动态调度算法<br>Operations-research-based fatigue-aware dynamic scheduling algorithm |
| **绿色化管控缺失<br>Missing Green Management** | 多数系统未对接绿色维修标准，无法自动归集环保数据<br>Most systems lack green maintenance standards; cannot auto-aggregate environmental data | 微观 ESG 碳账户 + 绿色评估引擎<br>Micro-ESG carbon account + green assessment engine |

## 三、应用场景与目标用户
## 3. Application Scenarios & Target Users

### 应用场景 / Application Scenarios
- **中小型维修门店及连锁维修机构 / Small and medium repair shops & chains**：统一管理用户、车辆、维修工单与人员调度 / Unified management of users, vehicles, repair orders, and personnel scheduling
- **线上线下融合的维修服务场景 / Online-to-offline repair service scenarios**：支持用户通过移动终端远程报修与初步诊断 / Supports remote repair reporting and preliminary diagnosis via mobile devices
- **高并发维修任务管理场景 / High-concurrency repair task management**：维修高峰期实现工单自动分配与负载均衡 / Automatic order assignment and load balancing during peak repair periods

### 目标用户 / Target Users
- **普通车主 / Car Owners**：缺乏专业维修知识，追求便捷报修、透明收费与进度可视化 / Lack professional repair knowledge; pursue convenient reporting, transparent pricing, and progress visualization
- **维修技师 / Repair Technicians**：关注工单分配公平性、收入透明度与技术经验积累 / Concerned with fair order assignment, income transparency, and skill accumulation
- **维修管理人员 / Repair Managers**：需要全局调度、数据决策与绿色合规管理能力 / Require global scheduling, data-driven decision-making, and green compliance management

## 四、竞品分析与差异化定位
## 4. Competitive Analysis & Differentiation

当前市场四类主流方案均存在明显短板，本项目通过"规则优先+AI 兜底"的混合技术路径实现差异化突破：
The four mainstream solution categories on the market all have notable shortcomings. This project achieves differentiated breakthrough via a "rules-first + AI fallback" hybrid technical approach:

| 竞品类型<br>Type | 代表案例<br>Example | 核心短板<br>Key Weakness | 本项目竞争优势<br>Our Advantage |
|----------|----------|----------|----------------|
| 传统管理型系统<br>Traditional Management | E 修工 | 人工依赖重、无智能调度、绿色功能缺失<br>Heavy manual reliance, no intelligent dispatch, missing green features | 多模态智能报修、疲劳算法派单、绿色评估模块<br>Multimodal intelligent reporting, fatigue-aware dispatch, green assessment module |
| 新型全域管理系统<br>All-in-One Management | 一家车联速修通 | 静态规则派单、算法层级低、功能堆叠<br>Static rule-based dispatch, low-level algorithms, feature bloat | 可解释贪心调度、数据闭环持续优化<br>Explainable greedy scheduling, data closed-loop continuous optimization |
| 品牌导向封闭式系统<br>Brand-Locked Closed Systems | 途虎养车、天猫养车 | 绑定品牌生态、算法黑箱、用户门槛高<br>Brand-ecosystem lock-in, algorithm black-box, high user barriers | 跨品牌通用、规则透明可配置、降低专业门槛<br>Cross-brand compatibility, transparent configurable rules, lower professional barriers |
| 纯 AI 通用系统<br>Pure AI General Systems | 腾讯元宝 | 无专业维修数据库、成本高、结果不可控<br>No professional repair database, high cost, uncontrollable results | 内置维修领域知识库、AI 仅兜底、计算成本降低 60%<br>Built-in repair domain knowledge base, AI as fallback only, 60% cost reduction |

## 五、系统架构
## 5. System Architecture

系统采用集中式部署、前后端分离的 B/S 架构，分为七层：
The system adopts a centralized deployment model with decoupled frontend-backend B/S architecture, organized into seven layers:

1. **前端展示与交互层 / Frontend Presentation & Interaction Layer**：Vue 2 + Vue Router + Vuex + Axios，多角色 Web 界面 / Multi-role web interfaces
2. **接入与通信分发层 / Access & Communication Distribution Layer**：Nginx 反向代理 + 静态资源承载 / Nginx reverse proxy + static resource serving
3. **业务逻辑服务层 / Business Logic Service Layer**：Spring Boot（Controller → Service → Repository），认证鉴权、工单生命周期、绿色 ESG 激励、预测性维护 / Authentication, order lifecycle, green ESG incentives, predictive maintenance
4. **混合智能诊断层 / Hybrid Intelligent Diagnosis Layer**（AI 大脑 / AI Brain）：
   - 规则预处理 / Rule Preprocessing — 正则 + 决策树，覆盖 90% 基础场景，毫秒级响应 / Regex + decision trees, covering 90% of basic scenarios with millisecond response
   - 多 Agent 协同推理 / Multi-Agent Collaborative Reasoning — 语义 Agent + 库存关联 Agent + 历史知识 Agent（RAG）/ Semantic Agent + Inventory Agent + Historical Knowledge Agent (RAG)
   - 决策综合融合 / Decision Fusion — 加权证据推理，消解跨模态冲突 / Weighted evidence reasoning to resolve cross-modal conflicts
   - 智能安全兜底 / Intelligent Safety Fallback — 置信度 < 0.85 自动挂起转人工复核 / Auto-suspend and escalate to manual review when confidence < 0.85
5. **动态隐私与安全层 / Dynamic Privacy & Security Layer**：旁路监听网关 + 本地正则引擎，车牌/VIN 无损脱敏 / Bypass monitoring gateway + local regex engine for lossless license plate/VIN masking
6. **算法与规则调度层 / Algorithm & Rule Scheduling Layer**：疲劳感知派单算法、微观 ESG 碳减排量化、游戏化积分里程换算 / Fatigue-aware dispatch algorithm, micro-ESG carbon emission quantification, gamification point-to-mileage conversion
7. **数据与缓存存储层 / Data & Cache Storage Layer**：MySQL 持久化 + H2 测试/一键运行包隔离 + Caffeine 热点缓存 / MySQL persistence + H2 isolation for tests and one-click packages + Caffeine hot-spot caching

## 六、功能模块
## 6. Functional Modules

| 模块<br>Module | 核心子功能<br>Core Sub-Functions | 说明<br>Description |
|------|-----------|------|
| **用户与认证管理<br>User & Auth** | 注册/登录、多角色识别、权限控制、Token 续期<br>Registration/login, multi-role identification, permission control, token renewal | JWT + 拦截器实现多角色登录态与归属校验<br>JWT + interceptors for multi-role session & ownership verification |
| **车辆与档案管理<br>Vehicle & Profile** | 车辆信息维护、历史维修记录查询、个人信息管理<br>Vehicle info maintenance, repair history query, personal profile management | 多车辆录入与维保记录回溯<br>Multi-vehicle registration & maintenance record traceability |
| **维修单与智能派单<br>Order & Smart Dispatch** | 在线报修、智能负载调度、工单全流转、催单与评价<br>Online repair reporting, intelligent load scheduling, full order lifecycle, reminders & reviews | 基于运筹学的疲劳感知动态派单<br>Operations-research-based fatigue-aware dynamic dispatch |
| **维保执行与物料<br>Repair Execution & Materials** | 维修过程记录、工时费用核算、物料消耗管理、库存预警<br>Repair process logging, labor cost accounting, material consumption tracking, inventory alerts | 动态追踪物料消耗，低阈值自动预警<br>Dynamic material tracking with low-threshold auto-alerts |
| **AI 智能诊断<br>AI Diagnosis** | 多模态故障输入、多轮 AI 问询、故障概率与结果输出<br>Multimodal fault input, multi-round AI consultation, fault probability & result output | 多阶段/多轮诊断 + 降级回退策略<br>Multi-stage/multi-round diagnosis + degradation fallback strategy |
| **绿色导向与激励<br>Green Initiative & Incentives** | 碳排放估算、绿色环保引导、节能任务激励<br>Carbon emission estimation, green behavior guidance, energy-saving task incentives | 微观 ESG 碳账户 + "零碳公路之旅"游戏化<br>Micro-ESG carbon account + "Zero-Carbon Road Trip" gamification |
| **预测性维护<br>Predictive Maintenance** | 保养提醒生成、车辆服务排期提示<br>Maintenance reminder generation, vehicle service scheduling | 定时扫描里程/时间，主动式"治未病"服务 / Periodic mileage/time scanning for proactive "preventive care" |
| **统计分析与系统管理<br>Analytics & Admin** | 运营数据仪表盘、跨表综合查询、异常监控、参数配置<br>Operations dashboard, cross-table query, anomaly monitoring, parameter configuration | 多维视图 + 派单权重等核心参数集中配置<br>Multi-dimensional views + centralized core parameter config |

## 七、核心功能与关键算法
## 7. Core Features & Key Algorithms

### 7.1 用户端 / Customer Side
- **爱车健康档案 / Vehicle Health Profile**：实时维修进度、费用明细、绿色等级评估报告 / Real-time repair progress, cost breakdown, green grade assessment report
- **多模态智能诊断助手 / Multimodal Intelligent Diagnosis Assistant**：深度集成 LLM + 计算机视觉，支持文字/照片/录音，秒级生成诊断报告 / Deeply integrated LLM + computer vision; supports text/photos/audio for instant diagnosis reports
- **"零碳公路之旅"游戏化互动 / "Zero-Carbon Road Trip" Gamification**：减排能量 → 绿色里程 → 地图闯关 → 知识问答 → 品牌福利抽奖 → 终极大奖 / Carbon reduction energy → green mileage → map challenges → knowledge quizzes → brand benefit lotteries → grand prize

### 7.2 技师端 / Technician Side
- **智能化作业与透明薪酬 / Intelligent Operations & Transparent Compensation**：自动精准推送工单，灵活接单/拒单，工时费 + 材料提成复合收益分析 / Auto-assigned precise order push, flexible accept/reject, labor + material commission composite income analysis
- **AI Copilot 诊断体系 / AI Copilot Diagnosis System**：规则优先识别高置信故障 → 多 Agent 协同攻克疑难杂症 → 敏感数据自动脱敏 → 决策融合输出置信度 / Rules-first high-confidence fault identification → multi-agent collaborative complex case resolution → auto data masking → decision fusion confidence output

### 7.3 管理端 / Admin Side
- **人机协同智能调度 / Human-AI Collaborative Intelligent Dispatch**：事件驱动架构 + 技能图谱 + 疲劳度模型动态派单 + 可视化人工干预 / Event-driven architecture + skill graph + fatigue model dynamic dispatch + visual manual intervention
- **多维数据聚合洞察 / Multi-Dimensional Data Analytics**：营收效能、高频故障态势、车型分布商业决策矩阵 + 绿色生态参数集中配置 / Revenue performance, high-frequency fault trends, vehicle model distribution business intelligence matrix + centralized green ecosystem parameter configuration

### 7.4 疲劳感知动态资源调度算法 / Fatigue-Aware Dynamic Resource Scheduling Algorithm
- **问题建模 / Problem Modeling**：带技能硬约束的启发式多目标优化（最小化等待时间 + 平衡负载）/ Heuristic multi-objective optimization with hard skill constraints (minimize wait time + balance workload)
- **评分模型 / Scoring Model**：`Score = 历史服务评分(50%) + 负载适配度(30%) + 经验匹配度(20%)` / `Score = Historical Service Rating (50%) + Load Adaptability (30%) + Experience Match (20%)`
- **疲劳度惩罚 / Fatigue Penalty**：基于"当日完成单量、连续作业时长、夜间工单占比"实时计算 Fatigue Level (0~1.0)，>0.7 触发拦截 / Real-time Fatigue Level (0–1.0) calculated from daily completed orders, continuous working hours, and nighttime order ratio; triggers interception when >0.7
- **反饥饿机制 / Anti-Starvation Mechanism**：长尾工单随驻留时间线性累加等待惩罚乘数，借鉴操作系统老化算法 / Long-tail orders linearly accumulate wait-penalty multipliers as queue residency increases, inspired by OS aging algorithms

### 7.5 绿色低碳评估算法引擎 / Green Low-Carbon Assessment Algorithm Engine
- **核心公式 / Core Formula**：`相对碳排量 = 预估工时 × 材料因子 × 维保策略因子 + 返工惩罚项(返工次数 × 1.5)` / `Relative Carbon Emission = Estimated Labor Hours × Material Factor × Repair Strategy Factor + Rework Penalty (rework count × 1.5)`
- **对比基准 / Baseline Benchmarks**：环保/常规材料因子 = 1.0/2.0，修复/更换策略因子 = 1.0/2.0（强引导"以修代换"和"绿色耗材"）/ Eco/Conventional material factor = 1.0/2.0; Repair/Replace strategy factor = 1.0/2.0 (strongly guiding "repair over replace" and "green consumables")
- **评级映射 / Rating Mapping**：Emission → S/A/B/C 四级绿色指数，联动前端展示碳减排收益 / Emission → S/A/B/C four-tier green index, linked to frontend carbon reduction benefit display

### 7.6 数据驱动的规则 + 多 Agent 协同混合故障诊断引擎 / Data-Driven Rules + Multi-Agent Collaborative Hybrid Fault Diagnosis Engine
- **规则预处理 / Rule Preprocessing**：正则 + 决策树，覆盖 90% 场景，毫秒级响应 / Regex + decision trees, 90% scenario coverage, millisecond response
- **三 Agent 并行推理 / Three-Agent Parallel Reasoning**：语义 Agent（方言/黑话解析）+ 库存 Agent（备货状态校验）+ 历史 Agent（RAG 检索相似案例）/ Semantic Agent (dialect/slang parsing) + Inventory Agent (stock verification) + Historical Agent (RAG-based similar case retrieval)
- **动态隐私熔断 / Dynamic Privacy Fusing**：本地正则扫描车牌/VIN，出站前无损脱敏 / Local regex scanning of license plates/VINs with lossless masking before external transmission
- **决策融合 / Decision Fusion**：后期融合策略加权证据推理，消解跨模态冲突 / Late fusion strategy with weighted evidence reasoning to resolve cross-modal conflicts
- **智能兜底 / Intelligent Fallback**：置信度 < 0.85 自动挂起转人工复核 / Auto-suspend and escalate to manual review when confidence < 0.85

### 7.7 基于微内核架构的数据闭环演进机制 / Microkernel-Based Data Closed-Loop Evolution Mechanism
- **Event-Bus 事件驱动 / Event-Bus Event-Driven**：FeedbackService 与 AutoAssignmentService 物理解耦，打通"AI 预诊—实体施工—偏差反馈"数据闭环 / Physically decouples FeedbackService and AutoAssignmentService, completing the "AI pre-diagnosis → physical repair → deviation feedback" data closed loop
- **人类在环样本沉淀 / Human-in-the-Loop Sample Accumulation**：技师确认的最终台账作为 Ground Truth，交叉比对沉淀长尾纠错样本 / Technician-confirmed final records serve as Ground Truth; cross-comparison accumulates long-tail correction samples
- **免重训自演进 / Zero-Retraining Self-Evolution**：动态 Prompt 模板热更新 + 调度算法权重动态回拨，零 GPU 算力实现算法可信度螺旋跃升 / Dynamic Prompt template hot-reload + scheduling algorithm weight dynamic callback, achieving algorithmic credibility spiral ascent with zero GPU cost

## 八、测试报告摘要
## 8. Test Report Summary

系统通过六大专项测试，验证了核心智能模块的工程可靠性与业务可行性：
The system passed six specialized tests, validating the engineering reliability and business feasibility of core intelligent modules:

| 测试项<br>Test Item | 关键指标<br>Key Metrics | 结果<br>Results |
|--------|----------|------|
| 多模态诊断交互<br>Multimodal Diagnosis Interaction | 语音 WER / 方言 CER / 图像 mAP@50<br>Speech WER / Dialect CER / Image mAP@50 | 4.6% / 3.1% / 0.986 |
| 混合诊断引擎高并发<br>Hybrid Diagnosis Engine High-Concurrency | 1000 QPS，1033 条工单<br>1000 QPS, 1033 orders | 规则拦截 68.2%，准确率 76.4%→**94.6%**，成本↓62.3%<br>Rule interception 68.2%, accuracy 76.4%→**94.6%**, cost ↓62.3% |
| 疲劳调度算法<br>Fatigue Scheduling Algorithm | 500 单/日仿真，1000 QPS<br>500 orders/day simulation, 1000 QPS | 饥饿消除 **100%**，等待↓24.5%，负载方差↓31%<br>Starvation elimination **100%**, wait time ↓24.5%, load variance ↓31% |
| 绿色碳排放引擎<br>Green Carbon Emission Engine | E2E 全链路集成测试<br>E2E full-chain integration test | 数据一致性 100%，跨模块冲突率 **0%**<br>Data consistency 100%, cross-module conflict rate **0%** |
| 零碳公路之旅<br>Zero-Carbon Road Trip | 并发幂等校验<br>Concurrent idempotency verification | 10 并发仅 1 成功，其余触发熔断，**零失误**<br>Only 1 of 10 concurrent requests succeeded; rest triggered circuit breaker; **zero errors** |
| 算法自演进闭环<br>Algorithm Self-Evolution Closed Loop | 1284 + 500 条数据，5 次迭代<br>1284 + 500 records, 5 iterations | 诊断↑8.2%，规则覆盖↑9.4%，调度匹配↑13.4%<br>Diagnosis ↑8.2%, rule coverage ↑9.4%, scheduling match ↑13.4% |

## 九、技术栈
## 9. Tech Stack

| 类别<br>Category | 技术<br>Technology | 版本<br>Version |
|------|------|------|
| **后端框架<br>Backend Framework** | Spring Boot + Security + Data JPA/Hibernate | 3.4.5 |
| **前端框架<br>Frontend Framework** | Vue 2 + Vue Router + Vuex + Axios | 2.6.14 / 3.5.1 / 3.6.2 / 1.9.0 |
| **数据库<br>Database** | MySQL（生产/Production）/ H2（测试与一键运行包/Test & One-click Package） | 8.4.8 / 2.x |
| **缓存<br>Cache** | Caffeine | — |
| **数据迁移<br>DB Migration** | Flyway | — |
| **API 文档<br>API Docs** | springdoc-openapi | 2.8.6 |
| **安全<br>Security** | JJWT + OkHttp + Jackson | 0.12.6 / 4.12.0 |
| **构建工具<br>Build Tools** | Maven Wrapper / npm | 3.9.9 |
| **测试<br>Testing** | JUnit / Playwright | 1.53.2 |
| **AI 模型<br>AI Models** | 小米 mimo v2.5 / DeepSeek V3 | — |

## 十、数据库设计
## 10. Database Design

### 核心实体 E-R 关系 / Core Entity E-R Relationships
- 用户(1) → 车辆(N)；用户(1) → 工单(N)；车辆(1) → 工单(N) / User(1) → Vehicle(N); User(1) → Order(N); Vehicle(1) → Order(N)
- 技师(N) ↔ 维修工单(N)；维修工单(1) → 反馈(N) / Technician(N) ↔ Repair Order(N); Repair Order(1) → Feedback(N)
- 用户(1) → 绿色能量账户(1)；用户(1) → 奖励台账(N) / User(1) → Green Energy Account(1); User(1) → Reward Ledger(N)
- 路线(1) → 路线节点(N)；用户+路线(1) → 节点状态(N) / Journey Map(1) → Map Node(N); User + Map(1) → Node State(N)

### 核心数据表 / Core Data Tables
`user` · `vehicles` · `technicians` · `repair_orders` · `order_technician` · `feedback` · `material` · `green_energy_account` · `green_reward_ledger` · `journey_map` · `journey_map_node` · `green_journey_node_state` · `green_quiz` · `coupon` · `user_coupon_wallet` · `maintenance_alert`

## 十一、接口设计
## 11. API Design

模块间通过 HTTP JSON 协议交换数据，RESTful 风格，统一响应结构：
Inter-module data exchange uses HTTP JSON protocol in RESTful style, with a unified response structure:

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功 / Operation successful",
  "data": {},
  "timestamp": 1717200000000
}
```

- `POST /api/ai-diagnosis/diagnose` — 提交故障描述获取 AI 诊断结果 / Submit fault description to obtain AI diagnosis results
- `PUT /api/repair-orders/{id}/status?status=COMPLETED` — 更新工单状态 / Update order status
- `GET /api/admins/dashboard-stats` — 管理端看板统计数据 / Admin dashboard statistics
- AI 诊断接口兼容 Chat Completions 协议；外部监管接口预留绿色数据上报能力 / AI diagnosis API is compatible with Chat Completions protocol; external regulatory API reserves green data reporting capability

## 十二、安装与使用
## 12. Installation & Usage

### 环境要求 / Prerequisites
- **普通用户 / End Users**：无需安装 Java、Node.js、Maven 或 MySQL，只需使用 Windows/macOS 一键运行包 / No Java, Node.js, Maven, or MySQL installation is required when using the Windows/macOS one-click package
- **打包开发者 / Release Builders**：JDK 17+、Node.js/npm、Bash、curl、zip、unzip、tar / JDK 17+, Node.js/npm, Bash, curl, zip, unzip, tar
- **浏览器 / Browser**：近两年主流 Chrome、Edge 或 Firefox / Recent mainstream Chrome, Edge, or Firefox (within 2 years)

### 快速启动 / Quick Start

#### 普通用户：一键运行包 / End Users: One-Click Package

下载并解压对应平台的运行包：

- Windows：双击 `启动系统.bat`
- macOS：双击 `启动系统.command`；如系统提示无法打开，请右键选择“打开”

启动后浏览器会自动打开：

```text
http://localhost:8080
```

默认演示账号 / Demo Accounts：

| 角色 / Role | 账号 / Username | 密码 / Password |
| --- | --- | --- |
| 管理员 / Admin | `admin` | `123456` |
| 车主 / Customer | `user` | `123456` |
| 技师 / Technician | `tech` | `123456` |
| 钣喷技师 / Bodywork Technician | `body` | `123456` |

运行包内置 Java 运行时，并使用本地文件数据库保存数据。默认首次启动会自动初始化上表列出的默认账号；若开发者打包时追加 `--sync-mysql-data`，则会优先导入包内 MySQL 业务数据快照。最终用户无需安装或配置 MySQL。

#### 开发者：生成 Windows/macOS 运行包 / Developers: Build Packages

```bash
chmod +x scripts/package-release.sh
scripts/package-release.sh --platform all
```

生成结果位于：

```text
dist/installers/
  fangxingwei-ai-windows-x64.zip
  fangxingwei-ai-macos-arm64.zip
  fangxingwei-ai-macos-x64.zip
```

Windows 包请直接使用脚本产物发布；脚本会保留 zip 的 UTF-8 中文文件名标志，避免 `启动系统.bat` 等文件在 Windows 解压后乱码。

需要让一键包内置当前 MySQL 业务数据时，可追加 `--sync-mysql-data`；最终用户仍使用包内本地数据库，无需安装 MySQL。

详细说明见 `docs/packaging/one_click_packaging.md`。

#### 开发/生产部署：MySQL 模式 / Development & Production: MySQL Mode

项目默认配置仍使用 MySQL，适合开发、测试和正式部署。请先按 `SQL/README.md` 初始化数据库，再启动后端和前端开发服务。

```bash
# 后端 / Backend
cd backend
./mvnw spring-boot:run

# 前端 / Frontend
cd frontend
npm install
npm run serve
```

### 各端使用流程 / Workflow by Role
- **用户端 / Customer**：注册登录 → 添加车辆 → AI 问诊/报修 → 创建预约 → 查看进度 → 确认费用并评价 → 查看碳评估 → 参与零碳公路之旅 / Register/Login → Add Vehicle → AI Consultation/Report Repair → Create Appointment → Track Progress → Confirm Cost & Review → View Carbon Assessment → Join Zero-Carbon Road Trip
- **技师端 / Technician**：登录 → 查看任务 → 接单 → 填写维修日志与材料 → 提交完成 → 查看收入 / Login → View Tasks → Accept Order → Log Repair & Materials → Submit Completion → View Income
- **管理端 / Admin**：登录 → 运营概览 → 工单监控与人工调整 → 技师/配置维护 → 绿色评估/优惠券/活动管理 / Login → Operations Overview → Monitor & Manually Adjust Orders → Maintain Technicians & Config → Manage Green Assessment/Coupons/Campaigns

## 十三、项目发展历程
## 13. Project Timeline

| 阶段<br>Phase | 时间<br>Time | 核心工作<br>Key Activities |
|------|------|----------|
| 前期调研<br>Preliminary Research | 2026.01 | 12 项门店痛点调研，对接多家维修门店<br>Researched 12 shop pain points; engaged multiple repair shops |
| 架构与基础开发<br>Architecture & Basic Development | 2026.01–03 | 前后端框架搭建，工单管理、基础派单<br>Frontend/backend framework setup; order management, basic dispatch |
| 创新点论证<br>Innovation Validation | 2026.03 | 疲劳算法、绿色评估、游戏化、数据闭环方向确定<br>Finalized directions: fatigue algorithm, green assessment, gamification, data closed loop |
| 功能落地<br>Feature Implementation | 2026.03–04 | 所有创新功能开发，代码优化<br>All innovative feature development; code optimization |
| **复旦大学路演<br>Fudan University Roadshow** | **2026.04.15** | 专场路演展示，获 21 条专业反馈<br>Dedicated roadshow presentation; received 21 pieces of expert feedback |
| 试运行<br>Trial Operation | 2026.04 | 3 家合作门店真实场景验证，37 条用户反馈<br>Real-world validation at 3 partner shops; collected 37 user feedback items |
| 数据反哺验证<br>Data Feedback Validation | 2026.05 | 真实场景数据对规则与算法的优化验证<br>Verified real-world data optimization effects on rules and algorithms |
| 版本封装<br>Final Packaging | 2026.05.27 | 最终封装归档，Git 72 次有效提交<br>Final system packaging and archival; 72 meaningful Git commits |

## 十四、项目核心价值
## 14. Core Value Proposition

- **行业赋能 / Industry Empowerment**：帮助中小维修企业以极低成本实现数字化+绿色化转型，提升运营效率 35% 以上，自动生成符合国标的绿色维修报告 / Help small and medium repair enterprises achieve digital + green transformation at very low cost, improving operational efficiency by over 35% and auto-generating nationally compliant green maintenance reports
- **技术创新 / Technical Innovation**：探索"规则优先 + 多 Agent 兜底"可控智能化路径，事件驱动高可用架构，为传统行业数字化提供可借鉴范式 / Pioneer a "rules-first + multi-agent fallback" controllable intelligence pathway with event-driven high-availability architecture, offering a replicable paradigm for traditional industry digitalization
- **社会公益 / Social Impact**：微观 ESG 体系引导绿色维修，预计年减排 120 万吨，疲劳感知调度保障从业者权益 / Micro-ESG system guides green maintenance practices, projected annual carbon reduction of 1.2 million tons; fatigue-aware scheduling protects worker well-being
- **商业生态 / Business Ecosystem**："门店-用户-品牌方"三方共赢，SaaS 订阅/绿色金融/广告分成/数据服务多元盈利，可推广至全国 300 万家门店 / "Shop-User-Brand" tripartite win-win; diverse monetization via SaaS subscriptions, green finance, advertising revenue sharing, and data services; scalable to 3 million repair shops nationwide

## 十五、改进方向
## 15. Improvement Directions

- **功能深化 / Feature Deepening**：扩充新能源三电维修规则库，对接动力电池溯源系统 / Expand new energy vehicle three-electric-system repair rule library; integrate with power battery traceability systems
- **技术迭代 / Technical Iteration**：数据库支持百万级工单，微服务架构支持跨区域多门店协同 / Support million-level orders in database; microservice architecture for cross-region multi-shop collaboration
- **生态扩展 / Ecosystem Expansion**：对接保险机构、汽车厂商、充电桩运营商，构建"维修+保险+出行"生态 / Integrate with insurers, automakers, and charging station operators to build a "Repair + Insurance + Mobility" ecosystem
- **商业化落地 / Commercialization**：轻量免费版 + 专业付费版，与区域汽修协会合作推广 / Lightweight free tier + professional paid tier; partner with regional auto repair associations for promotion

## 十六、AI 使用声明
## 16. AI Usage Statement

本项目开发中使用了 DeepSeek V3、豆包 AI 4.0 Pro、通义千问 Qwen 2.5 辅助架构梳理、行业分析与基础框架推导。所有 AI 生成代码均经过交叉 Code Review 及自动化集成测试验证，团队对核心算法（疲劳感知调度、绿色 ESG 碳排计算、多 Agent 混诊链路）进行了完全重构，AI 采纳比例严格控制在 20% 以内，项目核心知识产权归属团队。

During development, DeepSeek V3, Doubao AI 4.0 Pro, and Tongyi Qianwen Qwen 2.5 were used to assist with architecture review, industry analysis, and basic framework derivation. All AI-generated code underwent cross Code Review and automated integration test verification. The team completely re-architected the core algorithms (fatigue-aware scheduling, green ESG carbon calculation, multi-agent hybrid diagnosis pipeline), with AI adoption rate strictly controlled within 20%. Core intellectual property belongs to the team.

## 参考文献 / References

1. Ge H, et al. Vehicle fault maintenance system based on AI intelligent application technology. ICDSIS 2021.
2. Hossain M, et al. Artificial intelligence-driven vehicle fault diagnosis. CMES, 2024.
3. 杨琴等. 基于约束理论的汽车 4S 店维修服务系统动态调度. 中国管理科学, 2011.
4. 林泽民. 智能诊断技术在汽车故障检测中的应用与优化. 汽车电器, 2025.
5. Liang J S. A process-based automotive troubleshooting service. Robotics and CIM, 2020.
6. 金安鹏. 绿色维修技术在汽车维修中的应用策略研究. 内燃机与配件, 2024.
7. Nimmagadda V S P. Hybrid AI Architectures for End-to-End QA Automation. AJCCAS, 2020.
8. Mehra A. Hybrid AI models: Integrating symbolic reasoning with deep learning. JETIR, 2024.
9. Zielosko B, et al. Weighting attributes based on the greedy algorithm properties. Procedia CS, 2024.
10. 侯艺琦. 面向碳普惠的绿色出行碳积分激励策略研究. 东南大学, 2024.
11. Lifshitz S, et al. Multi-agent verification: Scaling test-time compute. arXiv, 2025.
12. Xu T, et al. Verification-aware planning for multi-agent systems. EACL, 2026.
