# 发布前检查单（Backend 优先）

适用仓库：vehicle-maintenance-management-system
最后更新：2026-03-30

## 1. 版本与变更冻结
- [ ] 冻结主分支（仅允许发布修复）
- [ ] 记录本次发布范围（功能、修复、已知问题）
- [ ] 确认本次发布不含数据库破坏性变更
- [ ] 生成发布标签（建议：vYYYY.MM.DD-build）

## 2. 构建与测试
在 backend 目录执行：

```powershell
.\mvnw.cmd clean test
```

```powershell
.\scripts\quality-gate.ps1
```

```powershell
.\scripts\release-gate.ps1 -BaseUrl http://localhost:8080
```

- [ ] 构建成功
- [ ] 单测/契约测试全绿（当前基线：Tests run 35, Failures 0, Errors 0）
- [ ] 随机并发用例稳定（GamificationServiceConcurrencyTest）
- [ ] 本地质量门禁通过（quality-gate）
- [ ] 标准化发布门禁通过（release-gate）

## 3. 运行时配置（必须项）
- [ ] 生产数据库连接与账号最小权限
- [ ] JWT 密钥与过期时间已配置（不可使用默认/弱密钥）
- [ ] CORS 白名单仅包含正式域名
- [ ] 日志级别：生产默认 INFO，避免 DEBUG 全局开启
- [ ] spring.jpa.open-in-view 按策略确认（建议关闭并评估影响）

## 4. 数据与迁移安全
- [ ] 生产库备份完成（含回滚时间点）
- [ ] 迁移脚本已在预发环境演练
- [ ] 迁移脚本序列校验通过：`cd backend && .\scripts\validate-migrations.ps1`
- [ ] 关键表索引与外键状态确认
- [ ] 初始化/测试数据脚本不会在生产误执行

## 5. 安全与鉴权检查
- [ ] 未授权请求返回 401/403 行为符合预期
- [ ] refresh token 轮换、吊销、设备绑定流程可用
- [ ] 鉴权拦截器异常日志可追踪（不泄露敏感信息）
- [ ] 管理员接口角色校验已覆盖关键路径

## 6. 可观测性与告警
- [ ] 应用启动日志无 ERROR
- [ ] 关键链路日志可定位：
  - [ ] RepairOrderController 异常路径
  - [ ] AdminController 统计降级路径
  - [ ] JWT 拦截失败路径
  - [ ] 刷新令牌/登出链路
- [ ] 接入监控：QPS、错误率、P95/P99、数据库连接池使用率
- [ ] 告警阈值已配置并完成演练

## 7. 业务验收冒烟（最小集）
- [ ] 登录（管理员/技师/用户）
- [ ] 车辆增删改查
- [ ] 工单创建、分配、状态流转、催单
- [ ] 技师拒单与重新分配
- [ ] 游戏化奖励结算、问答打卡、随机事件
- [ ] 管理员统计接口（正常+失败降级）
- [ ] 自动冒烟通过：`cd backend && .\scripts\post-release-smoke.ps1 -BaseUrl http://localhost:8080`

## 8. 回滚预案（必须可执行）
- [ ] 回滚触发条件已定义（如 5xx 持续超阈值）
- [ ] 回滚步骤已文档化并演练：
  1. 切换流量到上一稳定版本
  2. 停止当前版本实例
  3. 必要时恢复数据库快照/增量日志
  4. 验证核心接口与登录链路
- [ ] 回滚负责人、执行窗口、沟通群组已明确

## 9. 发布窗口执行单
- [ ] T-30 分钟：确认无未完成迁移任务
- [ ] T-10 分钟：冻结变更、通知相关方
- [ ] T+0：部署并观察 15 分钟
- [ ] T+15：执行冒烟
- [ ] T+30：观察监控曲线、确认放量
- [ ] T+60：发布结论与风险复盘
- [ ] 按灰度模板执行放量与回滚判据：`backend/GRAY_RELEASE_PLAYBOOK.md`

## 10. 发布后 24h 关注项
- [ ] 错误日志 Top10（去重）
- [ ] 接口耗时回归（特别是统计与并发奖励）
- [ ] 数据一致性抽样（工单、奖励、卡券状态）
- [ ] 用户反馈与客服工单聚类
- [ ] 告警响应与值班执行符合 SOP（见 `backend/OPERABILITY_RUNBOOK.md`）

---

## 一键复核命令（建议）

```powershell
# 后端测试
Set-Location backend
.\mvnw.cmd test -DskipTests=false

# 返回仓库根目录
Set-Location ..
```

## 备注
- 当前仓库已完成多轮“无破坏优化”，建议上线前仅做必要修复，避免新增功能插入发布窗口。
- 灰度放量建议统一使用模板：`backend/GRAY_RELEASE_PLAYBOOK.md`。
