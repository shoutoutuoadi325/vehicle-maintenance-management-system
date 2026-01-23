# AI智能诊断功能说明

## 功能概述

AI智能诊断是车辆维修管理系统中的关键功能，用于自动判断车辆故障需要哪种类型的维修技师。

## 诊断策略（三级诊断）

系统采用三级诊断策略，确保既能快速处理常见故障，又能智能处理特殊情况：

### 1. 关键字匹配（第一优先级）
- 当故障描述包含特定关键词时，直接匹配对应的技师类型
- 优点：响应快速，无需额外API调用
- 关键词映射：
  - **机械维修(MECHANIC)**: 发动机、变速箱、刹车、轮胎、悬挂、机械
  - **电气维修(ELECTRICIAN)**: 电路、电池、电子、音响、空调、灯光
  - **车身维修(BODY_WORK)**: 车身、碰撞、钣金、变形、修复
  - **喷漆(PAINT)**: 喷漆、油漆、外观、划痕、颜色
  - **故障诊断(DIAGNOSTIC)**: 诊断、检测、故障、检查

### 2. AI智能诊断（第二优先级）
- 当关键字匹配不到结果时，调用AI API进行智能判断
- AI会分析故障描述，返回最合适的技师类型
- 支持识别复杂或非标准的故障描述
- 可以识别无效输入

### 3. 默认诊断（兜底策略）
- 如果AI也无法判断，默认分配给诊断技师(DIAGNOSTIC)
- 确保系统始终能够处理任何输入

## API配置

系统支持多个可能的API base URL，并会自动尝试找到可用的URL：

```
- https://api.deerapi.com/v1/chat/completions
- https://api.deerapi.com/v1
- https://api.deerapi.com
```

### 配置文件

在 `application.properties` 中配置：

```properties
# AI API Configuration
ai.api.key=<your-api-key>
ai.api.model=gpt-3.5-turbo
```

> **注意**: 生产环境建议将API密钥移至环境变量或外部密钥管理系统

## 技术实现

### 核心类

1. **AIService.java**
   - 负责与AI API通信
   - 实现URL自动选择和缓存
   - 解析AI响应并转换为技师类型

2. **AutoAssignmentService.java**
   - 协调三级诊断策略
   - 调用关键字匹配和AI诊断
   - 返回最终的技师类型

### API请求格式

系统使用OpenAI兼容的API格式：

```json
{
  "model": "gpt-3.5-turbo",
  "messages": [
    {
      "role": "user",
      "content": "你是一个车辆维修诊断专家。请根据以下故障描述..."
    }
  ]
}
```

### 日志记录

系统使用SLF4J进行日志记录，便于追踪诊断过程：

```
INFO - 使用关键字匹配结果: [MECHANIC]
INFO - 关键字匹配未找到结果，调用AI进行智能诊断
INFO - AI API调用成功，使用URL: https://api.deerapi.com/v1/chat/completions
INFO - AI诊断结果: [ELECTRICIAN, DIAGNOSTIC]
```

## 使用示例

### 示例1：关键字匹配成功
```
输入: "发动机异响，需要检查"
输出: [MECHANIC, DIAGNOSTIC]
说明: 包含"发动机"和"检查"关键字，直接匹配成功
```

### 示例2：需要AI诊断
```
输入: "车子开不动了，不知道什么原因"
输出: [DIAGNOSTIC]
说明: 无明确关键字，AI分析后判断需要诊断
```

### 示例3：无效输入
```
输入: "你好"
输出: [DIAGNOSTIC]
说明: AI识别为无效输入，使用默认诊断类型
```

## 测试

项目包含完整的单元测试：

```bash
mvn test -Dtest=AIServiceTest
```

测试覆盖：
- AI响应解析逻辑
- 多种输入场景
- 边界情况处理

## 注意事项

1. **API密钥安全**: 生产环境请使用环境变量
2. **网络依赖**: AI诊断依赖外部API，确保网络连接
3. **降级策略**: 系统设计了完善的降级策略，即使AI服务不可用也能正常工作
4. **性能考虑**: 关键字匹配优先，减少不必要的API调用

## 未来改进方向

1. 支持更多的AI模型
2. 添加诊断结果缓存
3. 实现诊断结果的人工反馈学习
4. 支持多语言诊断
