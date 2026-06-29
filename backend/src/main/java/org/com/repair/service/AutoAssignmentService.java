package org.com.repair.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Technician;
import org.com.repair.entity.Technician.SkillType;
import org.com.repair.repository.DispatchWeightConfigRepository;
import org.com.repair.repository.TechnicianRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AutoAssignmentService {

    private static final String DEFAULT_DISPATCH_WEIGHT_CONFIG_KEY = "default";
    
    private final TechnicianRepository technicianRepository;
    private final FeedbackService feedbackService;
    private final DispatchWeightConfigRepository dispatchWeightConfigRepository;
    private final TechnicianService technicianService;
    private final AgingAntiStarvationDispatchPolicy agingPolicy;
    
    public AutoAssignmentService(TechnicianRepository technicianRepository,
                                 FeedbackService feedbackService,
                                 DispatchWeightConfigRepository dispatchWeightConfigRepository,
                                 @Lazy TechnicianService technicianService,
                                 AgingAntiStarvationDispatchPolicy agingPolicy) {
        this.technicianRepository = technicianRepository;
        this.feedbackService = feedbackService;
        this.dispatchWeightConfigRepository = dispatchWeightConfigRepository;
        this.technicianService = technicianService;
        this.agingPolicy = agingPolicy;
    }
    
    /**
     * 自动分配单个技师到维修工单（基于指定的技师工种类型）
     * @param requiredSkillType 需要的技能类型
     * @return 分配的技师
     */
    public Technician autoAssignBestTechnician(SkillType requiredSkillType) {
        return selectBestTechnician(requiredSkillType, null, null);
    }

    /**
     * 自动分配单个技师到具体维修工单。
     * 工单上下文用于 Aging 反饥饿调度和长尾工单识别。
     * @param repairOrder 维修工单
     * @return 分配的技师
     */
    public Technician autoAssignBestTechnician(RepairOrder repairOrder) {
        if (repairOrder == null || repairOrder.getRequiredSkillType() == null) {
            return null;
        }
        return selectBestTechnician(repairOrder.getRequiredSkillType(), repairOrder, null);
    }
    
    /**
     * 自动分配单个技师到维修工单（排除指定的技师）
     * @param requiredSkillType 需要的技能类型
     * @param excludeTechnicianId 要排除的技师ID
     * @return 分配的技师
     */
    public Technician autoAssignBestTechnicianExcluding(SkillType requiredSkillType, Long excludeTechnicianId) {
        return selectBestTechnician(requiredSkillType, null, excludeTechnicianId);
    }

    /**
     * 自动分配单个技师到具体维修工单（排除指定技师）。
     * @param repairOrder 维修工单
     * @param excludeTechnicianId 要排除的技师ID
     * @return 分配的技师
     */
    public Technician autoAssignBestTechnicianExcluding(RepairOrder repairOrder, Long excludeTechnicianId) {
        if (repairOrder == null || repairOrder.getRequiredSkillType() == null) {
            return null;
        }
        return selectBestTechnician(repairOrder.getRequiredSkillType(), repairOrder, excludeTechnicianId);
    }
    
    /**
     * 自动分配技师到维修工单（旧版本，保持兼容）
     * @param repairOrder 维修工单
     * @param requiredSkillTypes 需要的技能类型
     * @return 分配的技师集合
     */
    public Set<Technician> autoAssignTechnicians(RepairOrder repairOrder, Set<SkillType> requiredSkillTypes) {
        Set<Technician> assignedTechnicians = new HashSet<>();
        
        for (SkillType skillType : requiredSkillTypes) {
            Technician bestTechnician = selectBestTechnician(skillType, repairOrder, null);
            if (bestTechnician != null) {
                assignedTechnicians.add(bestTechnician);
            }
        }
        
        return assignedTechnicians;
    }

    /**
     * 按 Aging 反饥饿策略排列待分配工单。
     * 等待越久、越符合长尾特征的工单越靠前。
     */
    public List<RepairOrder> rankPendingOrdersByAging(List<RepairOrder> pendingOrders) {
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            return List.of();
        }

        Map<Long, RepairOrder> ordersById = new HashMap<>();
        for (RepairOrder order : pendingOrders) {
            if (order.getId() != null) {
                ordersById.put(order.getId(), order);
            }
        }

        List<RepairOrder> rankedOrders = agingPolicy.rankPendingOrders(pendingOrders, new Date())
                .stream()
                .map(priority -> ordersById.get(priority.orderId()))
                .filter(order -> order != null)
                .toList();

        if (rankedOrders.size() == pendingOrders.size()) {
            return rankedOrders;
        }

        return pendingOrders;
    }
    
    /**
     * 根据复杂度自动确定需要的技能类型
     * @param description 维修描述
     * @return 需要的技能类型集合
     */
    public Set<SkillType> determineRequiredSkillTypes(String description) {
        Set<SkillType> skillTypes = new HashSet<>();
        String lowerDesc = description.toLowerCase();
        
        // 基于关键词判断需要的技能类型
        if (lowerDesc.contains("发动机") || lowerDesc.contains("变速箱") || lowerDesc.contains("刹车") || 
            lowerDesc.contains("轮胎") || lowerDesc.contains("悬挂") || lowerDesc.contains("机械")) {
            skillTypes.add(SkillType.MECHANIC);
        }
        
        if (lowerDesc.contains("电路") || lowerDesc.contains("电池") || lowerDesc.contains("电子") || 
            lowerDesc.contains("音响") || lowerDesc.contains("空调") || lowerDesc.contains("灯光")) {
            skillTypes.add(SkillType.ELECTRICIAN);
        }
        
        if (lowerDesc.contains("车身") || lowerDesc.contains("碰撞") || lowerDesc.contains("钣金") || 
            lowerDesc.contains("变形") || lowerDesc.contains("修复")) {
            skillTypes.add(SkillType.BODY_WORK);
        }
        
        if (lowerDesc.contains("喷漆") || lowerDesc.contains("油漆") || lowerDesc.contains("外观") || 
            lowerDesc.contains("划痕") || lowerDesc.contains("颜色")) {
            skillTypes.add(SkillType.PAINT);
        }
        
        if (lowerDesc.contains("诊断") || lowerDesc.contains("检测") || lowerDesc.contains("故障") || 
            lowerDesc.contains("检查") || skillTypes.isEmpty()) {
            skillTypes.add(SkillType.DIAGNOSTIC);
        }
        
        return skillTypes;
    }
    
    /**
     * 计算技师评分
     * 评分规则：评分越高越好，工作负载越少越好，完成任务数量多更好
     * 疲劳度按配置权重扣分，防止高负荷技师继续被优先分配。
     */
    double calculateTechnicianScore(Technician technician) {
        DispatchScoringWeights weights = loadDispatchScoringWeights();
        return calculateTechnicianScore(technician, weights);
    }

    private double calculateTechnicianScore(Technician technician, DispatchScoringWeights weights) {
        // 历史服务评分可由管理员维护；未维护时回退到客户反馈平均分。
        double ratingScore = resolveHistoricalServiceRating(technician);
        
        // 获取当前工作负载（未完成的订单数量）
        int currentWorkload = getCurrentWorkload(technician);
        double workloadScore = Math.max(0, 10 - currentWorkload); // 工作负载越少分数越高
        
        // 获取完成任务数量
        int completedOrders = technician.getCompletedOrders() != null ? technician.getCompletedOrders() : 0;
        double experienceScore = Math.min(completedOrders * 0.1, 5.0); // 经验分数，最高5分

        double fatigueLevel = getFatigueLevel(technician);
        
        // 综合评分：正向评分权重来自 dispatch_weight_config；疲劳度作为惩罚项扣除。
        return (ratingScore * weights.ratingWeight())
                + (workloadScore * weights.workloadWeight())
                + (experienceScore * weights.experienceWeight())
                - (fatigueLevel * weights.fatiguePenaltyWeight());
    }

    private double resolveHistoricalServiceRating(Technician technician) {
        if (technician.getServiceRating() != null) {
            return technician.getServiceRating();
        }

        Double averageRating = feedbackService.getAverageRatingByTechnicianId(technician.getId());
        return averageRating != null ? averageRating : 3.0;
    }

    private Technician selectBestTechnician(SkillType requiredSkillType,
                                            RepairOrder repairOrder,
                                            Long excludeTechnicianId) {
        List<Technician> availableTechnicians = new ArrayList<>(technicianRepository.findBySkillType(requiredSkillType));

        if (availableTechnicians.isEmpty()) {
            return null;
        }

        if (excludeTechnicianId != null) {
            availableTechnicians.removeIf(technician -> technician.getId() != null
                    && technician.getId().equals(excludeTechnicianId));
        }

        if (availableTechnicians.isEmpty()) {
            return null;
        }

        // 计算每个技师的评分
        Map<Technician, Double> scores = new HashMap<>();

        Date now = new Date();
        DispatchScoringWeights weights = loadDispatchScoringWeights();
        for (Technician technician : availableTechnicians) {
            double baseScore = calculateTechnicianScore(technician, weights);
            double score = repairOrder == null
                    ? baseScore
                    : agingPolicy.calculateDispatchScore(baseScore, repairOrder, technician, now);
            scores.put(technician, score);
        }

        // 返回评分最高的技师
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private DispatchScoringWeights loadDispatchScoringWeights() {
        return dispatchWeightConfigRepository.findByConfigKeyAndEnabledTrue(DEFAULT_DISPATCH_WEIGHT_CONFIG_KEY)
                .map(config -> new DispatchScoringWeights(
                        toDouble(config.getRatingWeight(), DispatchScoringWeights.defaults().ratingWeight()),
                        toDouble(config.getWorkloadWeight(), DispatchScoringWeights.defaults().workloadWeight()),
                        toDouble(config.getExperienceWeight(), DispatchScoringWeights.defaults().experienceWeight()),
                        toDouble(config.getFatiguePenaltyWeight(), DispatchScoringWeights.defaults().fatiguePenaltyWeight())))
                .orElseGet(DispatchScoringWeights::defaults);
    }

    private double getFatigueLevel(Technician technician) {
        if (technician == null || technician.getId() == null) {
            return 0.0;
        }
        TechnicianService.TechnicianFatigueSnapshot snapshot =
                technicianService.getTechnicianFatigueSnapshot(technician.getId());
        return snapshot != null ? snapshot.getFatigueLevel() : 0.0;
    }

    private double toDouble(BigDecimal value, double fallback) {
        return value != null ? value.doubleValue() : fallback;
    }
    
    /**
     * 获取技师当前工作负载
     */
    private int getCurrentWorkload(Technician technician) {
        if (technician.getRepairOrders() == null) {
            return 0;
        }
        
        return (int) technician.getRepairOrders().stream()
                .filter(order -> order.getStatus() == RepairOrder.RepairStatus.ASSIGNED || 
                               order.getStatus() == RepairOrder.RepairStatus.IN_PROGRESS)
                .count();
    }
    
    /**
     * 估算维修工时
     */
    public double estimateWorkHours(String description, SkillType skillType) {
        String lowerDesc = description.toLowerCase();
        double baseHours = 2.0; // 基础工时
        
        // 根据技能类型调整基础工时
        switch (skillType) {
            case MECHANIC:
                baseHours = 4.0;
                break;
            case ELECTRICIAN:
                baseHours = 3.0;
                break;
            case BODY_WORK:
                baseHours = 6.0;
                break;
            case PAINT:
                baseHours = 8.0;
                break;
            case DIAGNOSTIC:
                baseHours = 1.5;
                break;
        }
        
        // 根据关键词调整工时
        if (lowerDesc.contains("大修") || lowerDesc.contains("更换发动机")) {
            baseHours *= 3;
        } else if (lowerDesc.contains("中修") || lowerDesc.contains("更换")) {
            baseHours *= 2;
        } else if (lowerDesc.contains("小修") || lowerDesc.contains("调整")) {
            baseHours *= 0.5;
        }
        
        return Math.max(0.5, baseHours); // 最少0.5小时
    }

    private record DispatchScoringWeights(
            double ratingWeight,
            double workloadWeight,
            double experienceWeight,
            double fatiguePenaltyWeight
    ) {
        private static DispatchScoringWeights defaults() {
            return new DispatchScoringWeights(0.5, 0.3, 0.2, 0.0);
        }
    }
}
