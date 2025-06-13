package org.com.repair.DTO;

import java.util.Date;

import org.com.repair.entity.Feedback;

public record FeedbackResponse(
    Long id,
    Integer rating,
    String comment,
    Date createdAt,
    RepairOrderInfo repairOrder,
    UserInfo user
) {
    public FeedbackResponse(Feedback feedback) {
        this(
            feedback.getId(),
            feedback.getRating(),
            feedback.getComment(),
            feedback.getCreatedAt(),
            feedback.getRepairOrder() != null ? new RepairOrderInfo(
                feedback.getRepairOrder().getId(),
                feedback.getRepairOrder().getOrderNumber(),
                feedback.getRepairOrder().getDescription()
            ) : null,
            feedback.getUser() != null ? new UserInfo(
                feedback.getUser().getId(),
                feedback.getUser().getName(),
                feedback.getUser().getUsername()
            ) : null
        );
    }
    
    public static record RepairOrderInfo(
        Long id,
        String orderNumber,
        String description
    ) {}
    
    public static record UserInfo(
        Long id,
        String name,
        String username
    ) {}
}