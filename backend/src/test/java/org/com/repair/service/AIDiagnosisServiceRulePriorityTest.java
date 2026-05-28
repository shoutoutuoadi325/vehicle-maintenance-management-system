package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AIDiagnosisServiceRulePriorityTest {

    @Test
    void technicianHighConfidenceRuleShouldReturnBeforeExternalAi() {
        AIDiagnosisService service = newService();

        AIDiagnosisResponse response = service.diagnoseFault("刹车变软，刹车距离变长", "technician", 7L);

        assertEquals("制动系统故障", response.getFaultType());
        assertEquals("CRITICAL", response.getSeverityLevel());
        assertTrue(response.getSuggestion().contains("命中规则：制动异常"));
    }

    @Test
    void adminRoleShouldUseTechnicianRulePriority() {
        AIDiagnosisService service = newService();

        AIDiagnosisResponse response = service.diagnoseFault("发动机舱冒烟，有焦糊味", "admin", 12L);

        assertEquals("高危电气或高温风险", response.getFaultType());
        assertEquals("CRITICAL", response.getSeverityLevel());
        assertTrue(response.getSuggestion().contains("命中规则：电气/高温风险"));
    }

    @Test
    void technicianMatchedRuleShouldNotShowAiUnavailableWhenExternalAiFails() {
        AIDiagnosisService service = newService();

        AIDiagnosisResponse response = service.diagnoseFault("冷车启动困难，偶尔打不着火", "technician", 7L);

        assertEquals("启动系统/发动机熄火异常", response.getFaultType());
        assertTrue(response.getSuggestion().contains("命中规则：启动/熄火异常"));
        assertFalse(response.getSuggestion().contains("外部AI服务当前连接不可用"));
    }

    private AIDiagnosisService newService() {
        return new AIDiagnosisService(
                mock(GamificationService.class),
                mock(TechnicianService.class),
                new RuleDiagnosisService());
    }
}
