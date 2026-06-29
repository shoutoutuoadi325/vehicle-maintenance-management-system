# 数据库测试数据使用说明

本文档包含测试帐号信息，以及如何在本地环境中重新构建数据库和注入测试数据的操作指南。

> **一键运行包说明：** 面向普通用户的 Windows/macOS 一键运行包使用 `standalone` Profile 和本地文件数据库，首次启动会自动建表；默认只写入根目录 `README.md` 中列出的默认账号。不要求用户安装 MySQL，也不需要手动执行本文档中的 SQL 命令。本文档仍适用于开发、测试和正式部署的 MySQL 模式。
> 如果打包时执行 `scripts/package-release.sh --sync-mysql-data`，脚本会从当前 MySQL 导出业务数据快照并放入安装包，安装包首次启动会优先导入该快照。

## 1. 预置测试帐号

数据初始化后，系统中将包含以下 4 个预置的核心角色测试账号。
所有账号的默认密码均为：**`123456`**

| 角色 | 账号 | 功能覆盖范围 |
| --- | --- | --- |
| **管理员** | `admin` | 总览指标、库存预警、最近订单、统计分析、调度看板 |
| **客户(车主)** | `user` | 车辆健康档案、保养提醒、维修进度、AI 问诊、零碳旅程 |
| **技师李浩** | `tech` | 个人任务、催单处理、收入统计、耗材消耗 |
| **技师赵宁** | `body` | 个人任务、催单处理、收入统计、耗材消耗 |

> *注：数据库 `user` 和 `technician` 等表中可能还包含其他供压测或特定场景验证使用的临时账号，但上述 4 个为标准业务链路演示账号。*

---

## 2. 重新注入测试数据的命令

如果你清空了数据库，或者测试导致了数据脏乱，想要恢复到初始状态（包含最新的 Schema 及全部基础测试数据），请在项目根目录下**完整执行**以下串联命令：

```bash
# 1. 强制重建空数据库
mysql -uroot -p79Haolubenwei -e "DROP DATABASE IF EXISTS car_repair; CREATE DATABASE car_repair;"

# 2. 导入代码生成的最新基础表结构
mysql -uroot -p79Haolubenwei car_repair < SQL/schema/backend_schema_from_code.sql

# 3. 运行 Flyway 完成版本迁移（由于已导入基础表，这里将 Baseline 设为 V22）
cd backend
sh mvnw flyway:baseline -Dflyway.url="jdbc:mysql://localhost:3306/car_repair?serverTimezone=Asia/Shanghai" -Dflyway.user=root -Dflyway.password=79Haolubenwei -Dflyway.baselineVersion=22
sh mvnw flyway:migrate -Dflyway.url="jdbc:mysql://localhost:3306/car_repair?serverTimezone=Asia/Shanghai" -Dflyway.user=root -Dflyway.password=79Haolubenwei
cd ..

# 4. 灌入最终核心业务测试数据
mysql -uroot -p79Haolubenwei car_repair < SQL/seed/competition_demo_seed.sql
```

`competition_demo_seed.sql` 会同步写入“COMP-318 川藏零碳服务线”的低碳旅程题库样例，包括 5 个城市默认题、5 个城市情景题和 2 个随机突发事件题，方便在客户端零碳旅程页面直接演示问答、打卡和权益闭环。

如果只需要补充/刷新低碳旅程题目，不想重置整套演示数据，可单独执行：

```bash
mysql -uroot -p79Haolubenwei car_repair < SQL/seed/low_carbon_journey_quiz_seed.sql
```

> **注意：**
> 1. 上述命令要求你的本机（或当前终端环境）中已安装 `mysql` 客户端工具。
> 2. 请根据你的实际本地数据库配置（账号/密码），替换掉上述命令中的 `-uroot -p79Haolubenwei` 等认证参数。
> 3. 所有 SQL 路径如 `SQL/schema/...` 为相对路径，需确保在**项目根目录（即 `/Users/zhiqizhang/development/vehicle-maintenance-management-system`）**下执行。

## 3. 与 Flyway 迁移相关的 AI 诊断表

最新迁移包含 `V25__add_technician_copilot_memory.sql`，会创建 `technician_copilot_memory` 表。该表用于保存每位技师的 AI Copilot 专属记忆，包括脱敏后的最近问题、故障类型、建议、历史案例 RAG 证据、置信度和工作流状态。开发或部署环境使用 `spring.jpa.hibernate.ddl-auto=validate` 时，必须先完成 Flyway 迁移再启动后端服务。
