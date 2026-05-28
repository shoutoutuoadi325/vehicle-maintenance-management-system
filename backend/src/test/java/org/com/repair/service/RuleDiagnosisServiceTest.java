package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleDiagnosisServiceTest {

    private final RuleDiagnosisService service = new RuleDiagnosisService();

    @Test
    void shouldDirectReturnForBrakeSoftProblem() {
        RuleDiagnosisService.RuleDiagnosisResult result = service.diagnose("刹车变软，刹车距离变长");

        assertTrue(result.matched());
        assertTrue(result.directReturn());
        assertEquals("制动异常", result.ruleHit());

        AIDiagnosisResponse response = result.response();
        assertNotNull(response);
        assertEquals("制动系统故障", response.getFaultType());
        assertEquals("CRITICAL", response.getSeverityLevel());
        assertEquals(800, response.getEstimatedCostMin());
        assertEquals(3000, response.getEstimatedCostMax());
        assertTrue(response.getSuggestion().contains("已根据技师端规则库完成初步判断"));
        assertFalse(response.getPossibleCauses().isEmpty());
    }

    @Test
    void shouldDirectReturnForEngineShakeAndFaultLight() {
        RuleDiagnosisService.RuleDiagnosisResult result = service.diagnose("发动机抖动，故障灯亮，加速动力不足");

        assertTrue(result.matched());
        assertTrue(result.directReturn());
        assertEquals("发动机运行异常", result.ruleHit());
        assertEquals("发动机运行异常/动力系统异常", result.response().getFaultType());
        assertEquals("HIGH", result.response().getSeverityLevel());
    }

    @Test
    void shouldDirectReturnForSmokeOrBurntSmell() {
        RuleDiagnosisService.RuleDiagnosisResult result = service.diagnose("发动机舱冒烟，有明显焦糊味");

        assertTrue(result.matched());
        assertTrue(result.directReturn());
        assertEquals("电气/高温风险", result.ruleHit());
        assertEquals("高危电气或高温风险", result.response().getFaultType());
        assertEquals("CRITICAL", result.response().getSeverityLevel());
    }

    @Test
    void shouldReturnNoHitForUnclearDescription() {
        RuleDiagnosisService.RuleDiagnosisResult result = service.diagnose("最近开起来感觉不太对");

        assertFalse(result.matched());
        assertFalse(result.directReturn());
    }

    @Test
    void shouldDirectReturnForStallProblem() {
        RuleDiagnosisService.RuleDiagnosisResult result = service.diagnose("为什么熄火");

        assertTrue(result.matched());
        assertTrue(result.directReturn());
        assertEquals("启动/熄火异常", result.ruleHit());
        assertEquals("启动系统/发动机熄火异常", result.response().getFaultType());
        assertTrue(result.response().getSuggestion().contains("命中规则：启动/熄火异常"));
    }
}
