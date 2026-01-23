package org.com.repair.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.com.repair.DTO.AIRequest;
import org.com.repair.DTO.AIResponse;
import org.com.repair.entity.Technician.SkillType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * AI智能诊断服务
 * 负责调用AI API进行故障诊断
 */
@Service
public class AIService {
    
    @Value("${ai.api.key:sk-YlfbboEmR0QGY8bl3bDf1h28NhCEdL4GhFxF9yhfri6UsHvc}")
    private String apiKey;
    
    @Value("${ai.api.model:gpt-3.5-turbo}")
    private String model;
    
    // 可能的API Base URLs
    private static final String[] POSSIBLE_BASE_URLS = {
        "https://api.deerapi.com/v1/chat/completions",
        "https://api.deerapi.com/v1",
        "https://api.deerapi.com"
    };
    
    private final RestTemplate restTemplate;
    private String workingBaseUrl = null;
    
    public AIService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 使用AI判断故障类型
     * @param description 故障描述
     * @return 推荐的技能类型集合
     */
    public Set<SkillType> determineSkillTypesByAI(String description) {
        try {
            String prompt = buildPrompt(description);
            String aiResponse = callAIAPI(prompt);
            return parseAIResponse(aiResponse);
        } catch (Exception e) {
            System.err.println("AI诊断失败: " + e.getMessage());
            // 如果AI调用失败，返回null，让调用方使用默认逻辑
            return null;
        }
    }
    
    /**
     * 构建AI提示词
     */
    private String buildPrompt(String description) {
        return "你是一个车辆维修诊断专家。请根据以下故障描述，判断需要哪种类型的维修技师。\n\n" +
               "故障描述：" + description + "\n\n" +
               "请从以下技师类型中选择一个或多个最合适的类型（只返回类型名称，用逗号分隔）：\n" +
               "- MECHANIC（机械维修）：适用于发动机、变速箱、刹车、轮胎、悬挂等机械问题\n" +
               "- ELECTRICIAN（电气维修）：适用于电路、电池、电子设备、音响、空调、灯光等电气问题\n" +
               "- BODY_WORK（车身维修）：适用于车身、碰撞、钣金、变形、修复等车身问题\n" +
               "- PAINT（喷漆）：适用于喷漆、油漆、外观、划痕、颜色等喷漆问题\n" +
               "- DIAGNOSTIC（故障诊断）：适用于需要诊断、检测、故障排查的情况\n\n" +
               "如果描述不是有效的车辆故障问题，请只回复\"INVALID\"。\n" +
               "请只回复技师类型名称（如：MECHANIC,ELECTRICIAN）或INVALID，不要有其他内容。";
    }
    
    /**
     * 调用AI API
     */
    private String callAIAPI(String prompt) throws Exception {
        // 构建请求体
        List<AIRequest.Message> messages = new ArrayList<>();
        messages.add(new AIRequest.Message("user", prompt));
        
        AIRequest request = new AIRequest(model, messages);
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        HttpEntity<AIRequest> entity = new HttpEntity<>(request, headers);
        
        // 尝试不同的base URL
        Exception lastException = null;
        for (String baseUrl : getPossibleUrls()) {
            try {
                ResponseEntity<AIResponse> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    AIResponse.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    AIResponse aiResponse = response.getBody();
                    if (aiResponse.getChoices() != null && !aiResponse.getChoices().isEmpty()) {
                        String content = aiResponse.getChoices().get(0).getMessage().getContent();
                        // 记录成功的URL，下次优先使用
                        workingBaseUrl = baseUrl;
                        System.out.println("AI API调用成功，使用URL: " + baseUrl);
                        return content;
                    }
                }
            } catch (Exception e) {
                lastException = e;
                System.err.println("尝试URL " + baseUrl + " 失败: " + e.getMessage());
            }
        }
        
        // 所有URL都失败了
        throw new Exception("所有AI API URL都无法访问", lastException);
    }
    
    /**
     * 获取可能的API URLs，优先使用之前成功的URL
     */
    private String[] getPossibleUrls() {
        if (workingBaseUrl != null) {
            // 如果已经找到可用的URL，优先使用它
            String[] urls = new String[POSSIBLE_BASE_URLS.length];
            urls[0] = workingBaseUrl;
            int index = 1;
            for (String url : POSSIBLE_BASE_URLS) {
                if (!url.equals(workingBaseUrl)) {
                    urls[index++] = url;
                }
            }
            return urls;
        }
        return POSSIBLE_BASE_URLS;
    }
    
    /**
     * 解析AI响应
     * Package-private for testing
     */
    Set<SkillType> parseAIResponse(String response) {
        Set<SkillType> skillTypes = new java.util.HashSet<>();
        
        if (response == null || response.trim().isEmpty()) {
            return skillTypes;
        }
        
        String trimmedResponse = response.trim().toUpperCase();
        
        // 检查是否为无效输入
        if (trimmedResponse.contains("INVALID")) {
            System.out.println("AI判定为无效输入");
            return skillTypes;
        }
        
        // 解析技能类型
        if (trimmedResponse.contains("MECHANIC")) {
            skillTypes.add(SkillType.MECHANIC);
        }
        if (trimmedResponse.contains("ELECTRICIAN")) {
            skillTypes.add(SkillType.ELECTRICIAN);
        }
        if (trimmedResponse.contains("BODY_WORK")) {
            skillTypes.add(SkillType.BODY_WORK);
        }
        if (trimmedResponse.contains("PAINT")) {
            skillTypes.add(SkillType.PAINT);
        }
        if (trimmedResponse.contains("DIAGNOSTIC")) {
            skillTypes.add(SkillType.DIAGNOSTIC);
        }
        
        return skillTypes;
    }
}
