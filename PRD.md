# **方行唯“AI”—智慧车辆维修管理系统 (补全计划 PRD)**

## **1\. 差距分析 (当前代码 vs 计划书目标)**

经过对当前代码库的审阅，系统目前已实现：用户/技师/管理员基础控制、工单流转、特色绿色零碳之旅（积分、问答、抽奖）、基础登录鉴权等。

**当前仍缺失或未完善的核心模块（即本次 PRD 重点）：**

1. **AI 智能问诊前端交互 (AI Diagnosis UX)**  
   * *现状*：后端已有 AIDiagnosisController 和 AIDiagnosisService，且有相关的 README，但前端代码中没有任何用于客户进行“AI问诊”的页面交互（如聊天框、故障描述输入、AI报告展示）。  
2. **基于混合 AI 的预测性维护 (Predictive Maintenance)**  
   * *现状*：计划书参考文献中提到了“Hybrid AI Architectures”和规则引擎，但目前后端仅有被动报修的 RepairOrder，缺乏基于车辆使用情况（如里程、时间）的**自动保养提醒**和**故障预测**功能。  
3. **智能派单可视化看板 (Smart Dispatch Dashboard)**  
   * *现状*：后端已有 AutoAssignmentService，但前端管理员/技师仪表盘较为简单，缺乏展示智能派单过程、技师负荷状态（Load Balancing）的动态看板或甘特图。  
4. **物料库存智能预警 (Material Inventory Alerts)**  
   * *现状*：有 Material 表和基础管理，但缺乏低于安全库存时的自动预警和采购建议功能。

## **2\. 产品需求详情 (User Stories & Acceptance Criteria)**

请 AI 编程助手基于以下需求，逐步为我生成前端 Vue3 组件和后端 Spring Boot 接口代码。

### **Epic 1: AI 智能问诊模块 (客户侧 & 技师侧)**

**目标**：将后端的 AI 诊断能力暴露给用户，提供类 ChatGPT 的交互体验。

* **User Story 1.1 (客户)**：作为车主，我希望能在“客户控制台”看到一个“AI 故障问诊”入口，可以通过文字描述（或语音转换）我的车辆异常现象。  
  * **验收标准**：  
    * 前端新增 AIDiagnosisClient.vue 组件，包含对话式 UI。  
    * 用户输入症状后，调用后端的 /api/diagnosis 接口。  
    * 界面能以 Markdown 或卡片形式展示 AI 给出的【可能原因】、【严重等级】和【预估维修费用/时间】。  
    * 提供“一键转化为维修工单”按钮，将 AI 诊断结果带入新建工单页面。  
* **User Story 1.2 (技师)**：作为技师，我希望在查看工单详情时，能看到 AI 针对该车辆历史维修记录和当前故障生成的“维修方案建议”。  
  * **验收标准**：  
    * 在技师的工单详情弹窗中新增 AI Suggestion 标签页。  
    * 展示基于规则库和历史数据的维修操作步骤建议。

### **Epic 2: 预测性维护与自动提醒 (系统级)**

**目标**：从“被动维修”向“主动保养”转变。

* **User Story 2.1 (系统/后端)**：作为系统，我需要每天定时检查所有车辆的里程数和上次保养时间，生成预测性维护建议。  
  * **验收标准**：  
    * 后端新增 Spring Boot 定时任务（@Scheduled）。  
    * 增加一张 MaintenanceAlert 表（包含车辆ID、提醒类型、触发时间、状态）。  
    * 规则：如距离上次保养超过 10000 公里或 6 个月，自动生成提醒。  
* **User Story 2.2 (客户)**：作为车主，我希望登录后能直观地看到我的爱车“健康状态”和近期的“保养/预警提示”。  
  * **验收标准**：  
    * 在 CustomerDashboard.vue 的显眼位置新增“爱车健康档案”模块。  
    * 如果存在未读的 MaintenanceAlert，显示红点或横幅提示。

### **Epic 3: 智能派单调度看板 (管理员侧)**

**目标**：让后端已经存在的 AutoAssignmentService 的效果在前端可视化。

* **User Story 3.1 (管理员)**：作为管理员，我希望有一个“智能调度看板”页面，看到所有技师的当前负荷、专长标签以及正在处理的工单。  
  * **验收标准**：  
    * 前端新增 DispatchBoard.vue 页面。  
    * 以 Kanban（看板）或时间轴的形式展示各位技师名下的工单。  
    * 突出显示“AI 自动分配”的工单标记。  
    * 允许管理员拖拽工单重新分配技师（覆盖 AI 的决定），并调用后端更新接口。

### **Epic 4: 绿色维修与物料库存预警 (绿色导向延伸)**

**目标**：结合绿色零碳主题，减少无效库存，提示采用环保配件。

* **User Story 4.1 (管理员)**：作为配件库管员，我希望系统能在某种物料库存数量低于阈值时报警。  
  * **验收标准**：  
    * 后端扩展 Material 实体，增加 minimum\_stock\_level (安全库存阈值) 字段。  
    * 当 RepairOrder 扣减库存导致数量低于阈值时，触发预警（记录到数据库或推送通知）。  
    * AdminDashboard.vue 中新增“库存预警”卡片列表。

## **3\. 技术实施指引 (提供给 AI 的开发约束)**

为了保持与现有代码库一致，请 AI 在生成代码时遵循以下规范：

1. **前端技术栈**：Vue 3 (Composition API, \<script setup\>)，Vue Router，Vuex（如果适用），界面样式使用现有项目中使用的 UI 库（推测为 Element Plus 或纯 CSS/Tailwind，请参考现有 AdminDashboard.vue 的风格）。  
2. **后端技术栈**：Spring Boot 3.x, Java 17+, JPA/Hibernate。  
3. **数据库迁移**：所有对数据库表的修改或新增，必须生成对应的 Flyway SQL 脚本（如 V16\_\_add\_maintenance\_alerts.sql）。  
4. **API 设计**：遵循 RESTful 风格，返回格式统包装为现有项目中的统一 Response 对象（如果有），请求和响应使用 DTO 类。  
5. **权限控制**：新的 Controller 接口必须加上现有的拦截器或权限注解（如涉及到用户的校验需经过 JwtAuthenticationInterceptor）。
