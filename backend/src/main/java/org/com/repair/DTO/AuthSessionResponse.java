package org.com.repair.DTO;

import java.time.LocalDateTime;

public record AuthSessionResponse(
        String deviceId,
        String deviceName,
        String ipAddress,
        LocalDateTime lastActiveAt,
        LocalDateTime expiresAt
) {
}