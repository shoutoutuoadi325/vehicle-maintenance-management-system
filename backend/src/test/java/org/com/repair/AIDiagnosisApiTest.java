package org.com.repair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI诊断API测试类
 * 用于测试deerapi服务是否可以正常调用
 */
@SpringBootTest
public class AIDiagnosisApiTest {

    @Value("${ai.diagnosis.api.key}")
    private String apiKey;

    @Value("${ai.diagnosis.api.base-url}")
    private String baseUrl;

    @Value("${ai.diagnosis.api.model}")
    private String model;

    @Test
    public void testDeerApiConnection() {
        System.out.println("=== 开始测试DeerAPI连接 ===");
        System.out.println("API Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "未配置"));
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Model: " + model);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        String testPrompt = "你好，请简单介绍一下你自己。";
        String requestBody = String.format(
            "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7}",
            model,
            testPrompt
        );

        System.out.println("请求内容: " + requestBody);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("响应状态码: " + response.code());
            
            String responseBody = response.body().string();
            System.out.println("响应内容: " + responseBody);

            assertTrue(response.isSuccessful(), "API调用应该成功");
            
            // 解析响应
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            assertTrue(jsonNode.has("choices"), "响应应该包含choices字段");
            assertTrue(jsonNode.path("choices").size() > 0, "choices应该至少有一个元素");
            
            String content = jsonNode.path("choices").get(0).path("message").path("content").asText();
            System.out.println("AI回复: " + content);
            
            assertFalse(content.isEmpty(), "AI回复不应该为空");
            
            System.out.println("=== DeerAPI连接测试通过 ===");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("API调用失败: " + e.getMessage());
        }
    }

    @Test
    public void testFaultDiagnosisPrompt() {
        System.out.println("=== 开始测试故障诊断功能 ===");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        String problemDescription = "汽车启动困难，发动机有异响，油耗增加";
        String prompt = "你是一个汽车维修专家。请根据以下问题描述，诊断可能的故障类型并给出建议。\\n\\n" +
                "问题描述：" + problemDescription + "\\n\\n" +
                "请用以下格式回复：\\n" +
                "故障类型：[具体的故障类型]\\n" +
                "建议：[详细的维修建议]";

        String requestBody = String.format(
            "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7}",
            model,
            prompt
        );

        System.out.println("测试问题: " + problemDescription);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("响应状态码: " + response.code());
            
            String responseBody = response.body().string();
            System.out.println("响应内容: " + responseBody);

            assertTrue(response.isSuccessful(), "API调用应该成功");
            
            // 解析响应
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            String content = jsonNode.path("choices").get(0).path("message").path("content").asText();
            System.out.println("=== AI诊断结果 ===");
            System.out.println(content);
            System.out.println("==================");
            
            assertFalse(content.isEmpty(), "诊断结果不应该为空");
            
            System.out.println("=== 故障诊断功能测试通过 ===");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("故障诊断测试失败: " + e.getMessage());
        }
    }
}
