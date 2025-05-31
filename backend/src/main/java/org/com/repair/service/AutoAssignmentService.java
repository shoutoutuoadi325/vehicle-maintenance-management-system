package org.com.repair.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Technician;
import org.com.repair.entity.Technician.SkillType;
import org.com.repair.repository.TechnicianRepository;
import org.springframework.stereotype.Service;

@Service
public class AutoAssignmentService {
    
    private final TechnicianRepository technicianRepository;
    
    public AutoAssignmentService(TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }
    
    /**
     * 自动分配技师到维修工单
     * @param repairOrder 维修工单
     * @param requiredSkillTypes 需要的技能类型
     * @return 分配的技师集合
     */
    public Set<Technician> autoAssignTechnicians(RepairOrder repairOrder, Set<SkillType> requiredSkillTypes) {
        Set<Technician> assignedTechnicians = new HashSet<>();
        
        for (SkillType skillType : requiredSkillTypes) {
            Technician bestTechnician = findBestAvailableTechnician(skillType);
            if (bestTechnician != null) {
                assignedTechnicians.add(bestTechnician);
            }
        }
        
        return assignedTechnicians;
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
     * 查找最合适的可用技师
     * 优先级：工作负载 > 时薪 > 经验
     */
    private Technician findBestAvailableTechnician(SkillType skillType) {
        List<Technician> availableTechnicians = technicianRepository.findAvailableTechnicians(skillType);
        
        if (availableTechnicians.isEmpty()) {
            return null;
        }
        
        // 计算每个技师的评分
        Map<Technician, Double> scores = new HashMap<>();
        
        for (Technician technician : availableTechnicians) {
            double score = calculateTechnicianScore(technician);
            scores.put(technician, score);
        }
        
        // 返回评分最高的技师
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * 计算技师评分
     * 评分规则：工作负载越少评分越高，时薪适中，完成任务数量多
     */
    private double calculateTechnicianScore(Technician technician) {
        double score = 100.0; // 基础分
        
        // 工作负载评分（权重40%）
        int activeTasksCount = technician.getRepairOrders() != null ? 
            (int) technician.getRepairOrders().stream()
                .filter(order -> !"COMPLETED".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus()))
                .count() : 0;
        
        double workloadScore = Math.max(0, 40 - (activeTasksCount * 8)); // 每个活跃任务减8分
        score += workloadScore * 0.4;
        
        // 时薪评分（权重20%） - 适中的时薪得分更高
        double hourlyRate = technician.getHourlyRate();
        double hourlyRateScore = 20 - Math.abs(hourlyRate - 80); // 假设80是理想时薪
        score += Math.max(0, hourlyRateScore) * 0.2;
        
        // 经验评分（权重40%） - 根据完成的任务数量
        int completedTasksCount = technician.getRepairOrders() != null ? 
            (int) technician.getRepairOrders().stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .count() : 0;
        
        double experienceScore = Math.min(40, completedTasksCount * 2); // 每完成一个任务加2分，最多40分
        score += experienceScore * 0.4;
        
        return score;
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
}
