package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一登录响应")
public record AuthLoginResponse<T>(
        @Schema(description = "JWT访问令牌")
        String accessToken,

        @Schema(description = "JWT刷新令牌")
        String refreshToken,

        @Schema(description = "用户角色")
        String role,

        @Schema(description = "登录用户详情")
        T user
) {
}
