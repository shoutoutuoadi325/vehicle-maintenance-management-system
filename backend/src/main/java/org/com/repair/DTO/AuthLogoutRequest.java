package org.com.repair.DTO;

import jakarta.validation.constraints.NotBlank;

public record AuthLogoutRequest(
        @NotBlank
        String refreshToken
) {
}
