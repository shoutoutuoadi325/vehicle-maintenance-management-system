# 数据库测试数据使用说明

本文档包含测试帐号信息，以及如何在本地环境中重新构建数据库和注入测试数据的操作指南。

## 1. 预置测试帐号

数据初始化后，系统中将包含以下 4 个预置的核心角色测试账号。
所有账号的默认密码均为：**`123456`**

| 角色 | 账号 | 功能覆盖范围 |
| --- | --- | --- |
| **管理员** | `admin` | 总览指标、库存预警、最近订单、统计分析、调度看板 |
| **客户(车主)** | `user` | 车辆健康档案、保养提醒、维修进度、AI 问诊、零碳旅程 |
| **技师李浩** | `tech` | 个人任务、催单处理、收入统计、耗材消耗 |
| **技师赵宁** | `body` | 个人任务、催单处理、收入统计、耗材消耗 |

> *注：数据库 `user` 和 `technician` 等表中可能还包含其他供压测或特定场景验证使用的临时账号，但上述 3 个为标准业务链路演示账号。*

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

> **注意：**
> 1. 上述命令要求你的本机（或当前终端环境）中已安装 `mysql` 客户端工具。
> 2. 请根据你的实际本地数据库配置（账号/密码），替换掉上述命令中的 `-uroot -p79Haolubenwei` 等认证参数。
> 3. 所有 SQL 路径如 `SQL/schema/...` 为相对路径，需确保在**项目根目录（即 `/Users/zhiqizhang/development/vehicle-maintenance-management-system`）**下执行。
