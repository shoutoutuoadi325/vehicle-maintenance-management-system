package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.com.repair.entity.Technician.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AI服务的单元测试
 */
public class AIServiceTest {
    
    private AIService aiService;
    
    @BeforeEach
    public void setUp() {
        aiService = new AIService();
    }
    
    @Test
    public void testParseAIResponse_WithMechanic() {
        // 测试解析包含MECHANIC的响应
        String response = "MECHANIC";
        Set<SkillType> result = aiService.parseAIResponse(response);
        
        assertNotNull(result);
        assertTrue(result.contains(SkillType.MECHANIC));
        assertEquals(1, result.size());
    }
    
    @Test
    public void testParseAIResponse_WithMultipleTypes() {
        // 测试解析包含多个类型的响应
        String response = "MECHANIC,ELECTRICIAN";
        Set<SkillType> result = aiService.parseAIResponse(response);
        
        assertNotNull(result);
        assertTrue(result.contains(SkillType.MECHANIC));
        assertTrue(result.contains(SkillType.ELECTRICIAN));
        assertEquals(2, result.size());
    }
    
    @Test
    public void testParseAIResponse_WithInvalid() {
        // 测试解析无效输入
        String response = "INVALID";
        Set<SkillType> result = aiService.parseAIResponse(response);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testParseAIResponse_WithEmpty() {
        // 测试解析空响应
        String response = "";
        Set<SkillType> result = aiService.parseAIResponse(response);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testParseAIResponse_WithNull() {
        // 测试解析null响应
        Set<SkillType> result = aiService.parseAIResponse(null);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
