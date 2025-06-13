package org.com.repair.DTO;

import java.util.Date;

public record NewFeedbackRequest(
    Integer rating,
    String comment,
    Date createdAt,
    Long repairOrderId,
    Long userId
) {
} 