# CLAUDE.md — 项目 AI 助手指引

## 项目背景

本项目源自中国大学生计算机设计大赛参赛作品**"方行唯'AI'——面向全国300万中小维修门店的智慧车辆维修管理系统"**（作品编号：2026091181，版本 v5）。

项目旨在为中小维修门店提供智能化车辆维修管理解决方案，解决行业四大痛点：
- 信息化程度有限，传统系统依赖人工填写
- 智能化能力不足，纯 AI 方案缺乏专业领域约束且结果不稳定
- 资源调度低效，工单分配依赖人工经验
- 绿色化管控缺失，无法自动归集环保数据

**注意**: 本文档仅覆盖 AI 诊断模块的架构设计。完整项目设计请参阅 `软件应用与开发类作品设计和开发文档.pdf` 或 [README.md](./README.md)。

## 项目概述

**方行唯 AI 智能汽车维修系统** — 全栈车辆维修管理系统，支持客户、技师、管理员三种角色。

- **后端**: Spring Boot 3.4.5 + JPA + Flyway + MySQL 8 + H2（测试/一键运行包）+ Java 17
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
│   └── packaging/
│       └── one_click_packaging.md  # Windows/macOS 一键运行包打包说明
│
├── SQL/                            # 所有数据库脚本
│   ├── README.md                   # 数据库脚本使用说明
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
│   │   ├── application-standalone.properties  # 一键运行包配置
│   │   └── db/migration/           # Flyway 迁移 V3-V24
│   ├── src/test/                   # JUnit 测试
│   └── scripts/                    # PowerShell 质量/发布脚本
│
├── scripts/
│   └── package-release.sh          # 生成 Windows/macOS 一键运行包
│
└── frontend/                       # Vue 2 前端
    ├── src/
    │   ├── main.js                 # Axios + JWT 刷新拦截器
    │   ├── App.vue
    │   ├── router/index.js
    │   ├── store/index.js
    │   ├── components/             # IdentitySelection, AuthForm, Chart
    │   └── views/                  # 7 个页面视图
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
2. **AI 诊断**: `AIDiagnosisService` 编排规则引擎 + 外部 LLM；车主端使用单次语义诊断，技师/admin 端非高置信规则直出场景通过单次 `AI_CONSILIUM` 请求完成 `PRE_DIAG → MAIN_AGENT → RED_TEAM → ARBITRATOR` 四阶段协同会诊，详见 [agents.md](./agents.md)
3. **零碳旅程游戏化**: `GamificationService`（~1560 行）管理能量账户、城市签到、答题、随机事件、优惠券、排行榜、大奖
4. **绿碳高级算法与评级映射**: `GreenEmissionEngine` 基于工时、材料因子、维保策略因子和返工惩罚因子计算工单相对碳排放，映射为 S/A/B/C 四级绿色指数；`EmissionCalculatorService` 维护生命周期碳数据重算节点，支持历史样本线性回归系数校准
5. **智能派单**: `AutoAssignmentService` 读取 `dispatch_weight_config` 动态权重，结合评分、工作负载、经验、`TechnicianService` 疲劳度快照和 `AgingAntiStarvationDispatchPolicy` 等待老化策略进行派单
6. **维保预警**: `MaintenanceAlertService` 定时扫描车辆里程/时间，生成维保提醒
7. **库存预警**: `MaterialService` 消耗库存时自动检测低库存并生成告警
8. **反馈自迭代**: `FeedbackSelfIterationService` 基于反馈数据生成派单权重和 AI Prompt 模板调整建议，每日 03:15 生成待审草案；管理员可通过 `/api/admins/ai-self-iteration/*` API 和前端 `AI 自演进` 页面审核，审批后写入 `agent_prompt_template_config` 与 `dispatch_weight_config`，派单侧消费已启用权重配置

### 安全模型

- **JWT 认证**: `JwtAuthenticationInterceptor` 验证 Bearer Token
- **角色授权**: `RoleAuthorizationInterceptor` 按路径限制角色（admin/customer/technician）
- **所有权校验**: `OwnershipAuthorizationInterceptor` 确保用户只能访问自己的数据
- **安全审计**: `SecurityAuditInterceptor` 记录请求 ID 和耗时

### 数据库

- **Schema 管理**: Flyway，基线版本 V22，当前迁移 V3-V24
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

# 启动一键包同款 standalone 模式（无需 MySQL）
cd backend && APP_DATA_DIR=../data ./mvnw spring-boot:run -Dspring-boot.run.profiles=standalone
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

一键运行包使用 `standalone` Profile 和本地 H2 文件数据库，首次启动自动建表；默认只注入根目录 `README.md` 中列出的默认账号，若打包时追加 `--sync-mysql-data` 则优先导入包内 MySQL 业务数据快照，不需要最终用户执行 SQL 初始化命令。

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
| `AI_DIAGNOSIS_API_BASE_URL` | AI 诊断 API 地址 | `https://api.xiaomimimo.com/v1` |
| `AI_DIAGNOSIS_API_MODEL` | AI 模型名称 | `mimo-v2.5` |
| `JWT_SECRET_KEY` | JWT 签名密钥 | (内置默认值) |
| `APP_PORT` | 一键运行包端口 | `8080` |
| `APP_DATA_DIR` | 一键运行包本地数据目录 | `~/.fangxingwei-ai` 或运行包内 `data/` |
| `STANDALONE_DEMO_DATA_ENABLED` | 是否启用 standalone 默认账号初始化 | `true` |

## 重要约定

1. **API 路径**: 所有接口以 `/api/` 开头，前端代理转发
2. **响应格式**: 统一使用 `ApiResponse<T>` 包装（success, code, message, data, timestamp）
3. **异常处理**: 游戏化模块使用 `GamificationException` + `GamificationErrorCode`；其他模块使用标准异常
4. **事件驱动**: 维修完成 → `EmissionReducedEvent` → 游戏化能量奖励；旅程签到 → `JourneyFootprintEvent` → 足迹记录
5. **幂等设计**: 游戏化奖励使用 `(sourceType, sourceId, actionKey)` 幂等键；维保预警使用 `dedupKey` 去重
6. **并发控制**: 库存消耗使用 `PESSIMISTIC_WRITE`，能量账户使用 `@Version` 乐观锁
7. **测试**: 使用 H2 内存数据库，测试配置在 `src/test/resources/application.properties`
8. **一键运行包**: 使用 `scripts/package-release.sh` 生成 Windows/macOS zip，运行时激活 `standalone` Profile，内置 JRE 并使用本地 H2 文件数据库；如需安装包初始数据同步当前 MySQL，打包时加 `--sync-mysql-data`；Windows zip 必须保留 UTF-8 中文文件名标志，避免 `启动系统.bat` 等文件在 Windows 解压后乱码，不要用普通 `zip -qr` 手工重压发布目录

## 文档与质量纪律

1. **文档同步更新**: 每次修改代码或文件后，必须检查项目内所有文档是否需要同步更新（如适用），并在必要时立即更新。需检查的文档包括但不限于：
   - `CLAUDE.md` — AI 助手指引
   - `agents.md` — AI Agent 架构文档
   - `README.md` — 项目需求与设计文档
   - `docs/` 目录下所有设计文档
   - `SQL/README.md` — 数据库脚本使用说明
   - 其他与修改相关的 Markdown 文档
   
   触发条件包括但不限于：新增/删除文件或目录、变更技术栈版本、调整架构设计、修改环境变量或配置项、新增/变更 API 接口、调整数据库结构、修改关键业务逻辑。

2. **代码验证**: 每次修改代码之后，必须进行检查与测试（如适用，运行相关单元测试或 E2E 测试），确保代码没有问题。

3. **文档二次复核**: 每次修改任何文档之后，必须进行二次复核，确认内容与代码实际状态一致、无遗漏或错误。
