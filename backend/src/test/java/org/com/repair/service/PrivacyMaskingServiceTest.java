package org.com.repair.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivacyMaskingServiceTest {

    private final PrivacyMaskingService service = new PrivacyMaskingService();

    @Test
    void shouldMaskLicensePlateAndVin() {
        PrivacyMaskingService.MaskingResult result = service.mask("沪B67890 发动机抖动，VIN 为 LSVFA49J232123456");

        assertTrue(result.changed());
        assertEquals(1, result.licensePlateCount());
        assertEquals(1, result.vinCount());
        assertFalse(result.maskedText().contains("沪B67890"));
        assertFalse(result.maskedText().contains("LSVFA49J232123456"));
        assertTrue(result.maskedText().contains("[MASKED_PLATE_1]"));
        assertTrue(result.maskedText().contains("[MASKED_VIN_1]"));
    }

    @Test
    void shouldLeaveTextUnchangedWhenNoSensitiveTokenExists() {
        PrivacyMaskingService.MaskingResult result = service.mask("发动机抖动，冷车启动困难");

        assertFalse(result.changed());
        assertEquals("发动机抖动，冷车启动困难", result.maskedText());
    }
}
