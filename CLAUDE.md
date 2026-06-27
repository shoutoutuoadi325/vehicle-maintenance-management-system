# CLAUDE.md — 项目 AI 助手指引

## 项目背景

本项目源自中国大学生计算机设计大赛参赛作品**"方行唯'AI'——面向全国300万中小维修门店的智慧车辆维修管理系统"**（作品编号：2026091181，版本 v5）。

项目旨在为中小维修门店提供智能化车辆维修管理解决方案，解决行业四大痛点：
- 信息化程度有限，传统系统依赖人工填写
- 智能化能力不足，纯 AI 方案缺乏专业领域约束且结果不稳定
- 资源调度低效，工单分配依赖人工经验
- 绿色化管控缺失，无法自动归集环保数据

**注意**: 本文档仅覆盖 AI 诊断模块的架构设计。完整项目设计请参阅 `软件应用与开发类作品设计和开发文档.pdf`。

## 项目概述

**方行唯 AI 智能汽车维修系统** — 全栈车辆维修管理系统，支持客户、技师、管理员三种角色。

- **后端**: Spring Boot 3.4.5 + JPA + Flyway + MySQL 8 + Java 17
- **前端**: Vue 2.6.14 + Vue Router 3.x + Vuex 3.x + Axios
- **AI 诊断**: 外部 LLM API（小米mimo v2.5）+ 规则引擎 + 多 Agent 协同
- **构建**: Maven（后端）、npm（前端）

## 目录结构

```
.
├── CLAUDE.md                       # 本文件 — AI 助手指引
├── agents.md                       # AI Agent 架构文档
├── README.md                       # 项目需求文档
├── LICENSE
│
├── docs/                           # 所有文档集中存放
│   ├── architecture/               # 架构设计
│   │   ├── agent-architecture.md   # Agent 架构流程图（Mermaid）
│   │   ├── EVENT_DRIVEN_DESIGN.md  # 事件驱动设计
│   │   └── ARCHITECTURE_DEBT_LEDGER.md  # 技术债务清单
│   ├── operations/                 # 运维发布
│   │   ├── RELEASE_CHECKLIST.md    # 发布检查清单
│   │   ├── GRAY_RELEASE_PLAYBOOK.md # 灰度发布手册
│   │   └── OPERABILITY_RUNBOOK.md  # 运维 SOP
│   ├── features/                   # 功能说明
│   │   ├── AI_DIAGNOSIS_README.md  # AI 诊断环境配置
│   │   ├── 绿色导向功能说明.md      # 碳排放评估功能
│   │   └── 数据库表结构说明.md      # 数据库表结构（仅覆盖原始 8 表）
│   └── ai-diagnosis/               # AI 诊断设计文档
│       ├── mission.md              # 需求与目标
│       ├── tech-stack.md           # 技术架构
│       ├── roadmap.md              # 8 阶段实施路线
│       └── plan.md                 # Demo 开发计划
│
├── SQL/                            # 所有数据库脚本
│   ├── schema/                     # Schema 定义
│   │   ├── car_repair.sql          # 完整数据库导出（Navicat 格式）
│   │   └── backend_schema_from_code.sql  # 权威 Schema DDL
│   ├── seed/                       # 种子数据
│   │   ├── material.sql            # 基础物料数据
│   │   ├── test-data.sql           # 最小测试数据
│   │   ├── demo-seed.sql           # 完整演示数据（幂等）
│   │   └── competition_demo_seed.sql  # 竞赛演示数据
│   └── trigger/                    # 删除触发器
│       ├── delete_user.sql
│       └── delete_repair_order.sql
│
├── backend/                        # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/org/com/repair/
│   │   ├── BackendApplication.java
│   │   ├── architecture/agent/     # HybridDiagnosisEngine（独立展示用）
│   │   ├── config/                 # CORS、缓存、拦截器注册
│   │   ├── controller/             # 13 个 REST 控制器
│   │   ├── DTO/                    # 50+ 请求/响应 Record 类
│   │   ├── entity/                 # 25 个 JPA 实体
│   │   ├── event/                  # Spring 事件
│   │   ├── exception/              # 游戏化错误码
│   │   ├── repository/             # 25 个 JpaRepository
│   │   ├── security/               # JWT + 角色授权 + 所有权校验
│   │   └── service/                # 业务逻辑（含 green/ 子包）
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/           # Flyway 迁移 V3-V23
│   ├── src/test/                   # JUnit 测试
│   └── scripts/                    # PowerShell 质量/发布脚本
│
└── frontend/                       # Vue 2 前端
    ├── src/
    │   ├── main.js                 # Axios + JWT 刷新拦截器
    │   ├── App.vue
    │   ├── router/index.js
    │   ├── store/index.js
    │   ├── components/             # IdentitySelection, AuthForm, Chart
    │   └── views/                  # 8 个页面视图
    ├── tests/e2e/                  # Playwright E2E 测试
    └── vue.config.js
```

## 后端架构

### 包结构与职责

| 包 | 职责 |
|---|------|
| `controller` | REST API，路径 `/api/**`，返回 `ApiResponse` 包装 |
| `service` | 业务逻辑，`@Transactional` 管理 |
| `repository` | JPA 数据访问，部分使用原生 SQL |
| `entity` | JPA 实体，使用 Lombok `@Data` `@Builder` |
| `DTO` | Java Record 类型的请求/响应对象 |
| `config` | Spring 配置（CORS、缓存、拦截器） |
| `security` | JWT 认证 + 角色授权 + 所有权校验拦截器链 |
| `event` | Spring ApplicationEvent 事件驱动解耦 |
| `exception` | 游戏化模块错误码体系 |

### 关键业务模块

1. **核心维修流程**: User → Vehicle → RepairOrder → Technician（多对多） → Feedback
2. **AI 诊断**: `AIDiagnosisService` 编排规则引擎 + 多 Agent + 外部 LLM，详见 [agents.md](./agents.md)
3. **零碳旅程游戏化**: `GamificationService`（~1560 行）管理能量账户、城市签到、答题、随机事件、优惠券、排行榜、大奖
4. **维保预警**: `MaintenanceAlertService` 定时扫描车辆里程/时间，生成维保提醒
5. **库存预警**: `MaterialService` 消耗库存时自动检测低库存并生成告警
6. **反馈自迭代**: `FeedbackSelfIterationService` 基于反馈数据自动调整派单权重和 AI Prompt 模板

### 安全模型

- **JWT 认证**: `JwtAuthenticationInterceptor` 验证 Bearer Token
- **角色授权**: `RoleAuthorizationInterceptor` 按路径限制角色（admin/customer/technician）
- **所有权校验**: `OwnershipAuthorizationInterceptor` 确保用户只能访问自己的数据
- **安全审计**: `SecurityAuditInterceptor` 记录请求 ID 和耗时

### 数据库

- **Schema 管理**: Flyway，基线版本 V22，当前迁移 V3-V23
- **JPA 配置**: `ddl-auto=validate`（仅验证，由 Flyway 管理结构）
- **锁策略**: 悲观锁用于库存消耗和游戏化状态，乐观锁用于能量账户和优惠券钱包
- **权威 Schema**: `SQL/schema/backend_schema_from_code.sql`

## 前端架构

- **路由守卫**: 基于 localStorage 中的 `user` 和 `userRole` 进行认证和角色匹配
- **状态管理**: 组件内 `data()` 管理，Vuex 未实际使用
- **API 调用**: Axios 实例，60 秒超时，JWT 自动注入，401 自动刷新 Token
- **图表**: 纯 Canvas 实现的 `Chart.vue`（柱状图、饼图）
- **拖拽**: `vuedraggable` 用于派单看板
- **Markdown**: `marked` 库用于 AI 诊断结果渲染

## 开发命令

### 后端

```bash
# 构建
cd backend && ./mvnw clean package -DskipTests

# 运行测试
cd backend && ./mvnw test

# 启动（需要 MySQL）
cd backend && ./mvnw spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run serve        # 开发服务器 http://localhost:3000
npm run build        # 生产构建
npm run test:e2e     # Playwright E2E 测试
```

### 数据库初始化

```bash
# 方式一：完整导入
mysql -u root -p < SQL/schema/car_repair.sql

# 方式二：Flyway 自动迁移（推荐，backend 启动时自动执行）

# 演示数据
mysql -u root -p car_repair < SQL/seed/demo-seed.sql
```

## 环境变量

| 变量 | 用途 | 默认值 |
|------|------|--------|
| `SPRING_DATASOURCE_URL` | 数据库连接 | `jdbc:mysql://localhost:3306/car_repair` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `root` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `root` |
| `AI_DIAGNOSIS_API_KEY` | AI 诊断 API 密钥 | (需配置) |
| `AI_DIAGNOSIS_API_BASE_URL` | AI 诊断 API 地址 | `https://api.apiyi.com` |
| `AI_DIAGNOSIS_API_MODEL` | AI 模型名称 | `mimo-v2.5` |
| `JWT_SECRET_KEY` | JWT 签名密钥 | (内置默认值) |

## 重要约定

1. **API 路径**: 所有接口以 `/api/` 开头，前端代理转发
2. **响应格式**: 统一使用 `ApiResponse<T>` 包装（success, code, message, data, timestamp）
3. **异常处理**: 游戏化模块使用 `GamificationException` + `GamificationErrorCode`；其他模块使用标准异常
4. **事件驱动**: 维修完成 → `EmissionReducedEvent` → 游戏化能量奖励；旅程签到 → `JourneyFootprintEvent` → 足迹记录
5. **幂等设计**: 游戏化奖励使用 `(sourceType, sourceId, actionKey)` 幂等键；维保预警使用 `dedupKey` 去重
6. **并发控制**: 库存消耗使用 `PESSIMISTIC_WRITE`，能量账户使用 `@Version` 乐观锁
7. **测试**: 使用 H2 内存数据库，测试配置在 `src/test/resources/application.properties`
