package org.com.repair.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.com.repair.DTO.NewRepairOrderRequest;
import org.com.repair.DTO.RepairOrderResponse;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.RepairOrder.RepairStatus;
import org.com.repair.entity.Technician;
import org.com.repair.entity.User;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.TechnicianRepository;
import org.com.repair.repository.UserRepository;
import org.com.repair.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepairOrderService {
    
    private final RepairOrderRepository repairOrderRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final TechnicianRepository technicianRepository;
    private final AutoAssignmentService autoAssignmentService;
    
    public RepairOrderService(
            RepairOrderRepository repairOrderRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            TechnicianRepository technicianRepository,
            AutoAssignmentService autoAssignmentService) {
        this.repairOrderRepository = repairOrderRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.technicianRepository = technicianRepository;
        this.autoAssignmentService = autoAssignmentService;
    }
    
    @Transactional
    public RepairOrderResponse createRepairOrder(NewRepairOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new RuntimeException("车辆不存在"));
        
        // 检查是否有对应工种的技师
        if (request.requiredSkillType() != null) {
            Technician assignedTechnician = autoAssignmentService.autoAssignBestTechnician(request.requiredSkillType());
            
            if (assignedTechnician == null) {
                // 获取工种名称用于错误提示
                String skillTypeName = getSkillTypeName(request.requiredSkillType());
                throw new RuntimeException(String.format("抱歉，暂时没有可用的%s技师。请稍后再试或选择其他维修类型。我们会尽快安排合适的技师为您服务。", skillTypeName));
            }
        }
        
        RepairOrder repairOrder = new RepairOrder();
        repairOrder.setOrderNumber(generateOrderNumber());
        repairOrder.setStatus(request.status() != null ? request.status() : RepairOrder.RepairStatus.PENDING);
        repairOrder.setDescription(request.description());
        repairOrder.setCreatedAt(request.createdAt() != null ? request.createdAt() : new Date());
        repairOrder.setUpdatedAt(new Date());
        repairOrder.setCompletedAt(request.completedAt());
        repairOrder.setLaborCost(request.laborCost());
        repairOrder.setMaterialCost(request.materialCost());
        repairOrder.setTotalCost(request.totalCost());
        repairOrder.setEstimatedHours(request.estimatedHours());
        repairOrder.setActualHours(request.actualHours());
        repairOrder.setUser(user);
        repairOrder.setVehicle(vehicle);
        repairOrder.setRequiredSkillType(request.requiredSkillType());
        
        // 自动分配技师（不再支持手动分配）
        if (request.requiredSkillType() != null) {
            Technician assignedTechnician = autoAssignmentService.autoAssignBestTechnician(request.requiredSkillType());
            
            Set<Technician> technicians = new HashSet<>();
            technicians.add(assignedTechnician);
            repairOrder.setTechnicians(technicians);
            repairOrder.setAssignmentType(RepairOrder.AssignmentType.AUTO);
            repairOrder.setStatus(RepairOrder.RepairStatus.ASSIGNED);
        }
        
        RepairOrder savedOrder = repairOrderRepository.save(repairOrder);
        return new RepairOrderResponse(savedOrder);
    }
    
    @Transactional
    public RepairOrderResponse reassignTechnicians(Long orderId, Set<Long> technicianIds, boolean isManual) {
        RepairOrder repairOrder = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修工单不存在"));
        
        Set<Technician> technicians = new HashSet<>();
        for (Long technicianId : technicianIds) {
            Technician technician = technicianRepository.findById(technicianId)
                    .orElseThrow(() -> new RuntimeException("技师不存在: " + technicianId));
            technicians.add(technician);
        }
        
        repairOrder.setTechnicians(technicians);
        repairOrder.setAssignmentType(isManual ? RepairOrder.AssignmentType.MANUAL : RepairOrder.AssignmentType.AUTO);
        repairOrder.setStatus(RepairOrder.RepairStatus.ASSIGNED);
        repairOrder.setUpdatedAt(new Date());
        
        RepairOrder updatedOrder = repairOrderRepository.save(repairOrder);
        return new RepairOrderResponse(updatedOrder);
    }
    
    @Transactional
    public RepairOrderResponse autoReassignTechnicians(Long orderId) {
        RepairOrder repairOrder = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修工单不存在"));
        
        // 重新进行自动分配
        Set<org.com.repair.entity.Technician.SkillType> requiredSkills = 
            autoAssignmentService.determineRequiredSkillTypes(repairOrder.getDescription());
        Set<Technician> assignedTechnicians = 
            autoAssignmentService.autoAssignTechnicians(repairOrder, requiredSkills);
        
        if (!assignedTechnicians.isEmpty()) {
            repairOrder.setTechnicians(assignedTechnicians);
            repairOrder.setAssignmentType(RepairOrder.AssignmentType.AUTO);
            repairOrder.setStatus(RepairOrder.RepairStatus.ASSIGNED);
            repairOrder.setUpdatedAt(new Date());
            
            // 重新计算预估工时
            double totalEstimatedHours = 0;
            for (org.com.repair.entity.Technician.SkillType skillType : requiredSkills) {
                totalEstimatedHours += autoAssignmentService.estimateWorkHours(repairOrder.getDescription(), skillType);
            }
            repairOrder.setEstimatedHours(totalEstimatedHours);
        }
        
        RepairOrder updatedOrder = repairOrderRepository.save(repairOrder);
        return new RepairOrderResponse(updatedOrder);
    }
    
    public Optional<RepairOrderResponse> getRepairOrderById(Long id) {
        return repairOrderRepository.findByIdWithAllDetails(id)
                .map(RepairOrderResponse::new);
    }
    
    public List<RepairOrderResponse> getRepairOrdersByUserId(Long userId) {
        return repairOrderRepository.findByUserIdWithDetails(userId).stream()
                .map(RepairOrderResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<RepairOrderResponse> getRepairOrdersByVehicleId(Long vehicleId) {
        return repairOrderRepository.findByVehicleId(vehicleId).stream()
                .map(RepairOrderResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<RepairOrderResponse> getRepairOrdersByTechnician(Long technicianId) {
        return repairOrderRepository.findByTechnicianId(technicianId).stream()
                .map(RepairOrderResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<RepairOrderResponse> getRepairOrdersByStatus(RepairStatus status) {
        return repairOrderRepository.findByStatus(status).stream()
                .map(RepairOrderResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<RepairOrderResponse> getUncompletedRepairOrders() {
        return repairOrderRepository.findUncompletedOrders().stream()
                .map(RepairOrderResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<RepairOrderResponse> getAllRepairOrders() {
        return repairOrderRepository.findAllWithDetails().stream()
                .map(RepairOrderResponse::new)
                .collect(Collectors.toList());
    }
    
    // AdminController需要的方法
    public List<RepairOrderResponse> getAllRepairOrdersWithDetails() {
        return repairOrderRepository.findAllWithDetails().stream()
                .map(RepairOrderResponse::new)
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> getDetailedStatistics(String startDate, String endDate) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            List<RepairOrder> allOrders;
            
            if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date start = dateFormat.parse(startDate);
                Date end = dateFormat.parse(endDate);
                allOrders = repairOrderRepository.findByCreatedAtBetween(start, end);
            } else {
                allOrders = repairOrderRepository.findAll();
            }
            
            // 基础统计
            long totalOrders = allOrders.size();
            long completedOrders = allOrders.stream()
                    .filter(order -> RepairStatus.COMPLETED.equals(order.getStatus()))
                    .count();
            long pendingOrders = allOrders.stream()
                    .filter(order -> RepairStatus.PENDING.equals(order.getStatus()))
                    .count();
            long inProgressOrders = allOrders.stream()
                    .filter(order -> RepairStatus.IN_PROGRESS.equals(order.getStatus()))
                    .count();
            
            // 费用统计
            double totalRevenue = allOrders.stream()
                    .filter(order -> order.getTotalCost() != null)
                    .mapToDouble(RepairOrder::getTotalCost)
                    .sum();
            
            double totalLaborCost = allOrders.stream()
                    .filter(order -> order.getLaborCost() != null)
                    .mapToDouble(RepairOrder::getLaborCost)
                    .sum();
            
            double totalMaterialCost = allOrders.stream()
                    .filter(order -> order.getMaterialCost() != null)
                    .mapToDouble(RepairOrder::getMaterialCost)
                    .sum();
            
            // 平均订单价值
            double averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;
            
            statistics.put("totalOrders", totalOrders);
            statistics.put("completedOrders", completedOrders);
            statistics.put("pendingOrders", pendingOrders);
            statistics.put("inProgressOrders", inProgressOrders);
            statistics.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
            statistics.put("laborCost", Math.round(totalLaborCost * 100.0) / 100.0);
            statistics.put("materialCost", Math.round(totalMaterialCost * 100.0) / 100.0);
            statistics.put("averageOrderValue", Math.round(averageOrderValue * 100.0) / 100.0);
            
            // 完成率
            double completionRate = totalOrders > 0 ? (double) completedOrders / totalOrders * 100 : 0;
            statistics.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
            
        } catch (Exception e) {
            // 返回默认统计数据
            statistics.put("totalOrders", 0);
            statistics.put("completedOrders", 0);
            statistics.put("pendingOrders", 0);
            statistics.put("inProgressOrders", 0);
            statistics.put("totalRevenue", 0.0);
            statistics.put("laborCost", 0.0);
            statistics.put("materialCost", 0.0);
            statistics.put("averageOrderValue", 0.0);
            statistics.put("completionRate", 0.0);
        }
        
        return statistics;
    }
    
    @Transactional
    public RepairOrderResponse updateRepairOrderStatus(Long orderId, RepairOrder.RepairStatus newStatus, Double materialCost) {
        RepairOrder repairOrder = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修订单不存在"));
        
        RepairOrder.RepairStatus oldStatus = repairOrder.getStatus();
        repairOrder.setStatus(newStatus);
        repairOrder.setUpdatedAt(new Date());
        
        // 如果从ASSIGNED状态变为IN_PROGRESS，记录开始时间
        if (oldStatus == RepairOrder.RepairStatus.ASSIGNED && newStatus == RepairOrder.RepairStatus.IN_PROGRESS) {
            repairOrder.setStartedAt(new Date());
        }
        
        // 如果从IN_PROGRESS状态变为COMPLETED，计算工时费和总费用
        if (oldStatus == RepairOrder.RepairStatus.IN_PROGRESS && newStatus == RepairOrder.RepairStatus.COMPLETED) {
            repairOrder.setCompletedAt(new Date());
            
            // 计算实际工作时间（小时）
            if (repairOrder.getStartedAt() != null) {
                long workTimeMillis = repairOrder.getCompletedAt().getTime() - repairOrder.getStartedAt().getTime();
                double actualHours = Math.ceil(workTimeMillis / (1000.0 * 60 * 60)); // 不满1小时按1小时计算
                repairOrder.setActualHours(actualHours);
                
                // 计算工时费
                if (repairOrder.getTechnicians() != null && !repairOrder.getTechnicians().isEmpty()) {
                    Technician technician = repairOrder.getTechnicians().iterator().next();
                    double laborCost = actualHours * technician.getHourlyRate();
                    repairOrder.setLaborCost(laborCost);
                }
            }
            
            // 设置材料费（由技师设定）
            if (materialCost != null) {
                repairOrder.setMaterialCost(materialCost);
            }
            
            // 计算总费用
            double totalCost = (repairOrder.getLaborCost() != null ? repairOrder.getLaborCost() : 0.0) +
                              (repairOrder.getMaterialCost() != null ? repairOrder.getMaterialCost() : 0.0);
            repairOrder.setTotalCost(totalCost);
            
            // 更新技师完成订单数量
            if (repairOrder.getTechnicians() != null && !repairOrder.getTechnicians().isEmpty()) {
                for (Technician technician : repairOrder.getTechnicians()) {
                    technician.setCompletedOrders((technician.getCompletedOrders() != null ? technician.getCompletedOrders() : 0) + 1);
                    technicianRepository.save(technician);
                }
            }
        }
        
        RepairOrder savedOrder = repairOrderRepository.save(repairOrder);
        return new RepairOrderResponse(savedOrder);
    }
    
    @Transactional
    public RepairOrderResponse updateRepairOrder(Long id, NewRepairOrderRequest request) {
        RepairOrder repairOrder = repairOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("维修工单不存在"));
        
        if (request.status() != null) {
            repairOrder.setStatus(request.status());
        }
        if (request.description() != null) {
            repairOrder.setDescription(request.description());
        }
        if (request.laborCost() != null) {
            repairOrder.setLaborCost(request.laborCost());
        }
        if (request.materialCost() != null) {
            repairOrder.setMaterialCost(request.materialCost());
        }
        if (request.totalCost() != null) {
            repairOrder.setTotalCost(request.totalCost());
        }
        
        repairOrder.setUpdatedAt(new Date());
        
        if (request.technicianIds() != null) {
            Set<Technician> technicians = new HashSet<>();
            for (Long technicianId : request.technicianIds()) {
                Technician technician = technicianRepository.findById(technicianId)
                        .orElseThrow(() -> new RuntimeException("技师不存在: " + technicianId));
                technicians.add(technician);
            }
            repairOrder.setTechnicians(technicians);
        }
        
        RepairOrder updatedOrder = repairOrderRepository.save(repairOrder);
        return new RepairOrderResponse(updatedOrder);
    }
    
    @Transactional
    public boolean deleteRepairOrder(Long id) {
        if (repairOrderRepository.existsById(id)) {
            repairOrderRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    // 统计分析方法
    public Object[] getQuarterlyCostAnalysis(int year, int quarter) {
        return repairOrderRepository.getQuarterlyCostAnalysis(year, quarter);
    }
    
    public Object[] getMonthlyCostAnalysis(int year, int month) {
        return repairOrderRepository.getMonthlyCostAnalysis(year, month);
    }
    
    public List<Object[]> getOrdersWithNegativeFeedback(int maxRating) {
        return repairOrderRepository.findOrdersWithNegativeFeedback(maxRating);
    }
    
    public List<Object[]> getTaskStatisticsBySkillType(Date startDate, Date endDate) {
        return repairOrderRepository.getTaskStatisticsBySkillType(startDate, endDate);
    }
    
    @Transactional
    public RepairOrderResponse urgeRepairOrder(Long orderId) {
        RepairOrder repairOrder = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修订单不存在"));

        // 检查订单状态是否为进行中
        if (repairOrder.getStatus() != RepairOrder.RepairStatus.IN_PROGRESS) {
            throw new RuntimeException("只有进行中的订单才能催单");
        }

        // 设置催单状态
        repairOrder.setUrgeStatus(RepairOrder.UrgeStatus.URGED);
        repairOrder.setUpdatedAt(new Date());
        
        return new RepairOrderResponse(repairOrderRepository.save(repairOrder));
    }
    
    private String generateOrderNumber() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateStr = dateFormat.format(new Date());
        long count = repairOrderRepository.count() + 1;
        return "RO" + dateStr + String.format("%04d", count);
    }
    
    /**
     * 获取技师工种类型的中文名称
     */
    private String getSkillTypeName(org.com.repair.entity.Technician.SkillType skillType) {
        switch (skillType) {
            case MECHANIC:
                return "机械维修";
            case ELECTRICIAN:
                return "电气维修";
            case BODY_WORK:
                return "车身维修";
            case PAINT:
                return "喷漆";
            case DIAGNOSTIC:
                return "故障诊断";
            default:
                return skillType.toString();
        }
    }
    
    /**
     * 重新分配所有未分配的订单
     * 用于管理员刷新数据时自动分配之前因技师不足而未分配的订单
     */
    @Transactional
    public List<RepairOrderResponse> reassignPendingOrders() {
        List<RepairOrderResponse> reassignedOrders = new ArrayList<>();
        
        // 查找所有状态为PENDING且有技师工种要求的订单
        List<RepairOrder> pendingOrders = repairOrderRepository.findByStatus(RepairOrder.RepairStatus.PENDING)
                .stream()
                .filter(order -> order.getRequiredSkillType() != null)
                .collect(Collectors.toList());
        
        int totalPendingOrders = pendingOrders.size();
        int successfulAssignments = 0;
        
        for (RepairOrder order : pendingOrders) {
            try {
                // 尝试为订单分配技师
                Technician assignedTechnician = autoAssignmentService.autoAssignBestTechnician(order.getRequiredSkillType());
                
                if (assignedTechnician != null) {
                    Set<Technician> technicians = new HashSet<>();
                    technicians.add(assignedTechnician);
                    order.setTechnicians(technicians);
                    order.setAssignmentType(RepairOrder.AssignmentType.AUTO);
                    order.setStatus(RepairOrder.RepairStatus.ASSIGNED);
                    order.setUpdatedAt(new Date());
                    
                    RepairOrder savedOrder = repairOrderRepository.save(order);
                    reassignedOrders.add(new RepairOrderResponse(savedOrder));
                    successfulAssignments++;
                }
            } catch (Exception e) {
                // 记录错误但继续处理其他订单
                System.err.println("重新分配订单 " + order.getId() + " 失败: " + e.getMessage());
            }
        }
        
        System.out.println(String.format("重新分配完成：总计 %d 个待分配订单，成功分配 %d 个", 
                totalPendingOrders, successfulAssignments));
        
        return reassignedOrders;
    }
    
    /**
     * 统计各车辆品牌的维修数量
     */
    public List<Map<String, Object>> getVehicleBrandRepairStatistics() {
        List<Map<String, Object>> statistics = new ArrayList<>();
        
        try {
            // 获取所有维修订单及其车辆信息
            List<RepairOrder> allOrders = repairOrderRepository.findAllWithDetails();
            
            // 统计各品牌的维修数量
            Map<String, Integer> brandCounts = new HashMap<>();
            
            for (RepairOrder order : allOrders) {
                if (order.getVehicle() != null && order.getVehicle().getBrand() != null) {
                    String brand = order.getVehicle().getBrand();
                    brandCounts.put(brand, brandCounts.getOrDefault(brand, 0) + 1);
                }
            }
            
            // 转换为统计结果并按维修数量排序
            for (Map.Entry<String, Integer> entry : brandCounts.entrySet()) {
                Map<String, Object> brandStat = new HashMap<>();
                brandStat.put("brand", entry.getKey());
                brandStat.put("repairCount", entry.getValue());
                statistics.add(brandStat);
            }
            
            // 按维修数量降序排序
            statistics.sort((a, b) -> Integer.compare((Integer) b.get("repairCount"), (Integer) a.get("repairCount")));
            
        } catch (Exception e) {
            System.err.println("获取车辆品牌维修统计失败: " + e.getMessage());
        }
        
        return statistics;
    }
    
    /**
     * 统计各维修工种类型的订单数量
     */
    public List<Map<String, Object>> getSkillTypeRepairStatistics() {
        List<Map<String, Object>> statistics = new ArrayList<>();
        
        try {
            // 获取所有维修订单
            List<RepairOrder> allOrders = repairOrderRepository.findAll();
            
            // 统计各工种类型的订单数量
            Map<String, Integer> skillTypeCounts = new HashMap<>();
            
            for (RepairOrder order : allOrders) {
                if (order.getRequiredSkillType() != null) {
                    String skillType = order.getRequiredSkillType().toString();
                    String skillTypeName = getSkillTypeName(order.getRequiredSkillType());
                    skillTypeCounts.put(skillTypeName, skillTypeCounts.getOrDefault(skillTypeName, 0) + 1);
                }
            }
            
            // 转换为统计结果
            for (Map.Entry<String, Integer> entry : skillTypeCounts.entrySet()) {
                Map<String, Object> skillTypeStat = new HashMap<>();
                skillTypeStat.put("skillType", entry.getKey());
                skillTypeStat.put("orderCount", entry.getValue());
                statistics.add(skillTypeStat);
            }
            
            // 按订单数量降序排序
            statistics.sort((a, b) -> Integer.compare((Integer) b.get("orderCount"), (Integer) a.get("orderCount")));
            
        } catch (Exception e) {
            System.err.println("获取工种类型维修统计失败: " + e.getMessage());
        }
        
        return statistics;
    }
}