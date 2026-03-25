package org.com.repair.DTO;

import jakarta.validation.constraints.NotBlank;

public record AuthRefreshRequest(
        @NotBlank
        String refreshToken
) {
}
