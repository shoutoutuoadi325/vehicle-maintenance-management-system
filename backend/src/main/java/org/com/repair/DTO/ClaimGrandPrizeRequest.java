package org.com.repair.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "通关终极奖励申领请求")
public record ClaimGrandPrizeRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "收货人姓名", example = "张三")
        String consigneeName,

        @NotBlank
        @Pattern(regexp = "^[0-9+\\- ]{6,30}$")
        @Schema(description = "联系电话", example = "13800138000")
        String consigneePhone,

        @NotBlank
        @Size(max = 500)
        @Schema(description = "详细收货地址", example = "四川省成都市高新区天府大道88号")
        String shippingAddress
) {
}
