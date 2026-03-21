package org.com.repair.event;

import org.springframework.context.ApplicationEvent;

/**
 * 减排事件
 * 当维修工单完成时发布此事件，用于触发游戏化模块的能量奖励
 */
public class EmissionReducedEvent extends ApplicationEvent {

    private Long repairOrderId;
    private Long userId;
    private Double emissionReduction; // kg CO2

    public EmissionReducedEvent(Object source, Long repairOrderId, Long userId, Double emissionReduction) {
        super(source);
        this.repairOrderId = repairOrderId;
        this.userId = userId;
        this.emissionReduction = emissionReduction;
    }

    public Long getRepairOrderId() {
        return repairOrderId;
    }

    public Long getUserId() {
        return userId;
    }

    public Double getEmissionReduction() {
        return emissionReduction;
    }

    @Override
    public String toString() {
        return String.format(
            "EmissionReducedEvent{repairOrderId=%d, userId=%d, emissionReduction=%.2f}",
            repairOrderId, userId, emissionReduction
        );
    }
}
