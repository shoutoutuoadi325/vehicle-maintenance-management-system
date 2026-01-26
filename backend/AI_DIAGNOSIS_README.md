# AI故障诊断功能文档

## 功能概述
本系统集成了AI故障诊断功能，允许用户输入车辆问题描述，系统将调用DeerAPI提供的AI服务给出故障类型和维修建议。

## API端点
- **POST** `/api/ai-diagnosis/diagnose`

### 请求体示例
```json
{
  "problemDescription": "汽车启动困难，发动机有异响，油耗增加"
}
```

### 响应示例（成功）
```json
{
  "faultType": "发动机故障",
  "suggestion": "可能是点火系统或燃油系统问题，建议检查火花塞、点火线圈和燃油泵",
  "success": true,
  "errorMessage": null
}
```

### 响应示例（失败）
```json
{
  "faultType": null,
  "suggestion": null,
  "success": false,
  "errorMessage": "AI诊断服务暂时不可用，请稍后再试"
}
```

## 配置说明

### 环境变量配置（推荐）
在生产环境中，强烈建议使用环境变量配置API密钥：

```bash
export AI_DIAGNOSIS_API_KEY=your-api-key-here
export AI_DIAGNOSIS_API_BASE_URL=https://api.deerapi.com/v1
export AI_DIAGNOSIS_API_MODEL=gpt-4.1
```

### application.properties配置
如果未设置环境变量，系统将使用application.properties中的默认值：

```properties
ai.diagnosis.api.key=${AI_DIAGNOSIS_API_KEY:sk-YlfbboEmR0QGY8bl3bDf1h28NhCEdL4GhFxF9yhfri6UsHvc}
ai.diagnosis.api.base-url=${AI_DIAGNOSIS_API_BASE_URL:https://api.deerapi.com/v1}
ai.diagnosis.api.model=${AI_DIAGNOSIS_API_MODEL:gpt-4.1}
```

**安全提醒：** 
- 当前配置文件中包含默认的API密钥用于开发和测试
- 在生产环境部署前，请务必通过环境变量设置自己的API密钥
- 建议将application.properties添加到.gitignore（如果包含敏感信息）

## API测试

### 运行测试
项目包含了AIDiagnosisApiTest测试类，用于测试DeerAPI服务的连接和功能。

默认情况下，这些测试是禁用的（使用@Disabled注解），因为它们需要：
1. 网络连接
2. 有效的API密钥
3. DeerAPI服务可用

要在本地环境运行测试：
1. 设置环境变量或使用默认配置
2. 移除测试类中的@Disabled注解
3. 运行测试：`mvn test -Dtest=AIDiagnosisApiTest`

### 手动测试
使用curl或Postman测试API：

```bash
curl -X POST http://localhost:8080/api/ai-diagnosis/diagnose \
  -H "Content-Type: application/json" \
  -d '{"problemDescription":"汽车启动困难，发动机有异响，油耗增加"}'
```

## DeerAPI集成说明

### API详情
- **服务提供商**: DeerAPI
- **基础URL**: https://api.deerapi.com/v1
- **模型**: gpt-4.1
- **令牌**: sk-YlfbboEmR0QGY8bl3bDf1h28NhCEdL4GhFxF9yhfri6UsHvc（开发环境）

### 技术实现
- 使用OkHttp客户端进行HTTP请求
- JSON序列化/反序列化使用Jackson
- 超时设置：连接30秒，读取60秒，写入30秒

## 错误处理
系统实现了完善的错误处理机制：
- API调用失败时返回通用错误消息（不暴露内部异常详情）
- 所有异常都会被记录到日志中供调试
- 返回的错误响应包含success标志和errorMessage字段

## 输入验证
- 问题描述字段不能为空（使用@NotBlank验证）
- 通过Spring的@Valid注解自动验证请求
- 验证失败时返回HTTP 400 Bad Request
