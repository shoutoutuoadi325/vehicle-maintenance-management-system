package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "管理员更新通关奖励发货状态请求")
public record AdminUpdateJourneyShipmentRequest(
        @NotBlank
        @Schema(description = "发货状态: PREPARING/SHIPPED/DELIVERED", example = "SHIPPED")
        String shippingStatus,

        @Size(max = 80)
        @Schema(description = "物流单号", example = "SF1234567890")
        String shipmentTrackingNo
) {
}
