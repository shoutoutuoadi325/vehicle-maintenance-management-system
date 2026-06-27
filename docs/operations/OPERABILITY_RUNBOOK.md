# 运行可观测闭环与值班 SOP

适用范围：backend 服务
最后更新：2026-03-31

## 1. 日志事件码规范

统一格式：

- `alertEvent=<EVENT_CODE>`
- `source=<scan-source or module>`
- `elapsedMs=<duration>`
- 业务关键上下文（如 `vehicleId`、`userId`）

当前已落地事件码：

- `MAINTENANCE_ALERT_SCAN_SUMMARY`
- `MAINTENANCE_ALERT_SCAN_FAILURE`

新增日志事件码时需满足：

1. 全大写蛇形命名。
2. 不包含业务敏感信息。
3. 能唯一定位到一个运行环节。

## 2. 指标与阈值

推荐落地以下 SLI/SLO 指标：

- 可用性：5xx 比例（5分钟窗口）
- 延迟：P95 / P99（按核心接口）
- 错误：关键事件码错误次数（按模块）
- 资源：JVM 堆使用率、数据库连接池使用率

告警阈值建议：

1. `5xx_rate_5m >= 2%`：P1
2. `api_p95_ms_10m >= 1500`：P2
3. `MAINTENANCE_ALERT_SCAN_FAILURE count >= 3 in 10m`：P2
4. `db_pool_usage >= 85% for 10m`：P2

## 3. 告警路由

1. P1：电话 + IM 群 + 值班负责人。
2. P2：IM 群 + 值班工程师。
3. P3：工作时段通知 + 下个迭代修复。

## 4. 值班响应 SOP

1. 1分钟内确认告警并标注负责人。
2. 5分钟内判断影响范围：用户、接口、数据一致性。
3. 10分钟内给出处置方向：回滚 / 限流 / 热修。
4. 若触发回滚条件（持续 5xx 超阈值），按发布回滚预案执行。
5. 故障恢复后 24h 内提交复盘：根因、修复、预防项。

## 5. 发布后自动冒烟

执行：

```powershell
Set-Location .\backend
.\scripts\post-release-smoke.ps1 -BaseUrl http://localhost:8080
```

要求：

1. 冒烟失败时发布不放量。
2. 冒烟通过后再进入观察窗口。

## 6. 审计与追踪

每次告警处置需记录：

- 告警时间
- 事件码
- 影响范围
- 响应人与决策时间
- 最终动作（回滚/修复/忽略）

建议将该记录汇总到发布复盘文档中。
