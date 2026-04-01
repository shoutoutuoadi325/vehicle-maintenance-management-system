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

import org.com.repair.DTO.DispatchBoardOrderResponse;
import org.com.repair.DTO.DispatchBoardTechnicianResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.com.repair.event.EmissionReducedEvent;

@Service
@SuppressWarnings({"null", "unused"})
public class RepairOrderService {
    private static final Logger logger = LoggerFactory.getLogger(RepairOrderService.class);

    private final RepairOrderRepository repairOrderRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final TechnicianRepository technicianRepository;
    private final AutoAssignmentService autoAssignmentService;
    private final EmissionCalculatorService emissionCalculatorService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public RepairOrderService(
            RepairOrderRepository repairOrderRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            TechnicianRepository technicianRepository,
            AutoAssignmentService autoAssignmentService,
            EmissionCalculatorService emissionCalculatorService,
            ApplicationEventPublisher eventPublisher) {
        this.repairOrderRepository = repairOrderRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.technicianRepository = technicianRepository;
        this.autoAssignmentService = autoAssignmentService;
        this.emissionCalculatorService = emissionCalculatorService;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderResponse createRepairOrder(NewRepairOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new RuntimeException("车辆不存在"));

        Set<RepairStatus> activeStatuses = Set.of(
                RepairStatus.PENDING,
                RepairStatus.ASSIGNED,
                RepairStatus.IN_PROGRESS);
        if (repairOrderRepository.existsByVehicleIdAndStatusIn(vehicle.getId(), activeStatuses)) {
            throw new RuntimeException("该车辆已有进行中的维修工单，请等待当前维修完成后再提交");
        }
        
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
        repairOrder.setRepairEndedAt(request.completedAt());
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
        
        // 设置绿色导向相关字段（可根据request补充）
        repairOrder.setEcoMaterial(request.ecoMaterial() != null ? request.ecoMaterial() : false);
        repairOrder.setReworkCount(request.reworkCount() != null ? request.reworkCount() : 0);
        repairOrder.setRepairType(request.repairType() != null ? request.repairType() : "repair");
        // 计算碳排放
        repairOrder.setEstimatedEmission(
            emissionCalculatorService.calculate(repairOrder)
        );

        RepairOrder savedOrder = repairOrderRepository.save(repairOrder);
        return new RepairOrderResponse(savedOrder);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderResponse reassignTechnicians(Long orderId, Set<Long> technicianIds, boolean isManual) {
        RepairOrder repairOrder = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修工单不存在"));
        Technician.SkillType requiredSkillType = repairOrder.getRequiredSkillType();

        Set<Long> previousTechnicianIds = repairOrder.getTechnicians() == null
            ? new HashSet<>()
            : repairOrder.getTechnicians().stream().map(Technician::getId).collect(Collectors.toSet());
        
        Set<Technician> technicians = new HashSet<>();
        for (Long technicianId : technicianIds) {
            Technician technician = technicianRepository.findById(technicianId)
                    .orElseThrow(() -> new RuntimeException("技师不存在: " + technicianId));

            if (requiredSkillType != null && technician.getSkillType() != requiredSkillType) {
            String requiredSkillTypeName = getSkillTypeName(requiredSkillType);
            String selectedSkillTypeName = technician.getSkillType() == null
                ? "未知工种"
                : getSkillTypeName(technician.getSkillType());
            throw new RuntimeException(String.format(
                "技师工种不匹配：该工单需要%s技师，当前选择为%s",
                requiredSkillTypeName,
                selectedSkillTypeName));
            }

            technicians.add(technician);
        }
        
        repairOrder.setTechnicians(technicians);
        repairOrder.setAssignmentType(isManual ? RepairOrder.AssignmentType.MANUAL : RepairOrder.AssignmentType.AUTO);
        repairOrder.setStatus(RepairOrder.RepairStatus.ASSIGNED);
        repairOrder.setUpdatedAt(new Date());
        
        RepairOrder updatedOrder = repairOrderRepository.save(repairOrder);

        Set<Long> affectedTechnicianIds = new HashSet<>(previousTechnicianIds);
        affectedTechnicianIds.addAll(technicians.stream().map(Technician::getId).collect(Collectors.toSet()));
        refreshTechnicianLoadStats(affectedTechnicianIds);

        return new RepairOrderResponse(updatedOrder);
    }

    private void refreshTechnicianLoadStats(Set<Long> technicianIds) {
        for (Long technicianId : technicianIds) {
            Technician technician = technicianRepository.findById(technicianId).orElse(null);
            if (technician == null) {
                continue;
            }

            List<RepairOrder> tasks = repairOrderRepository.findByTechnicianId(technicianId);
            double totalHours = tasks.stream()
                    .map(RepairOrder::getActualHours)
                    .filter(hours -> hours != null && hours > 0)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            int completedOrders = (int) tasks.stream()
                    .filter(order -> order.getStatus() == RepairOrder.RepairStatus.COMPLETED)
                    .count();

            technician.setTotalWorkHours(Math.round(totalHours * 100.0) / 100.0);
            technician.setCompletedOrders(completedOrders);
            technicianRepository.save(technician);
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
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

    public List<DispatchBoardTechnicianResponse> getDispatchBoard() {
        List<Technician> technicians = technicianRepository.findAll();
        List<RepairOrder> activeOrders = new ArrayList<>();
        activeOrders.addAll(repairOrderRepository.findByStatus(RepairStatus.ASSIGNED));
        activeOrders.addAll(repairOrderRepository.findByStatus(RepairStatus.IN_PROGRESS));

        Map<Long, List<DispatchBoardOrderResponse>> boardMap = new HashMap<>();
        for (RepairOrder order : activeOrders) {
            if (order.getTechnicians() == null || order.getTechnicians().isEmpty()) {
                continue;
            }

            DispatchBoardOrderResponse orderCard = new DispatchBoardOrderResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus() != null ? order.getStatus().name() : null,
                    order.getDescription(),
                    order.getRequiredSkillType() != null ? order.getRequiredSkillType().name() : null,
                    order.getUser() != null ? order.getUser().getName() : null,
                    order.getVehicle() != null ? order.getVehicle().getLicensePlate() : null,
                    order.getAssignmentType() != null ? order.getAssignmentType().name() : null,
                    order.getAssignmentType() == RepairOrder.AssignmentType.AUTO,
                    order.getCreatedAt(),
                    order.getEstimatedHours()
            );

            for (Technician tech : order.getTechnicians()) {
                boardMap.computeIfAbsent(tech.getId(), k -> new ArrayList<>()).add(orderCard);
            }
        }

        return technicians.stream()
                .map(tech -> {
                    List<DispatchBoardOrderResponse> techOrders = boardMap.getOrDefault(tech.getId(), new ArrayList<>());
                    return new DispatchBoardTechnicianResponse(
                            tech.getId(),
                            tech.getName(),
                            tech.getEmployeeId(),
                            tech.getSkillType() != null ? tech.getSkillType().name() : null,
                            tech.getHourlyRate(),
                            techOrders.size(),
                            tech.getTotalWorkHours(),
                            tech.getCompletedOrders(),
                            techOrders
                    );
                })
                .sorted((a, b) -> Integer.compare(b.activeOrderCount(), a.activeOrderCount()))
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
            logger.error("Failed to calculate detailed statistics, startDate={}, endDate={}", startDate, endDate, e);
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
    
    @Transactional(rollbackFor = Exception.class)
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
            repairOrder.setRepairEndedAt(repairOrder.getCompletedAt());
            
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
            
            // 发布减排事件，触发游戏化模块的能量奖励
            Double emissionReduction = repairOrder.getEstimatedEmission();
            if (emissionReduction != null && emissionReduction > 0 && repairOrder.getUser() != null) {
                EmissionReducedEvent event = new EmissionReducedEvent(
                    this,
                    repairOrder.getId(),
                    repairOrder.getUser().getId(),
                    emissionReduction
                );
                eventPublisher.publishEvent(event);
            }
        }
        
        RepairOrder savedOrder = repairOrderRepository.save(repairOrder);
        return new RepairOrderResponse(savedOrder);
    }
    
    @Transactional(rollbackFor = Exception.class)
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

        if (repairOrder.getStatus() == RepairOrder.RepairStatus.COMPLETED && repairOrder.getRepairEndedAt() == null) {
            repairOrder.setRepairEndedAt(new Date());
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
    
    @Transactional(rollbackFor = Exception.class)
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
    
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
                logger.warn("Reassign pending order failed, orderId={}", order.getId(), e);
            }
        }
        
        logger.info("Reassign pending orders completed: total={}, successful={}",
                totalPendingOrders, successfulAssignments);
        
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
            logger.error("Failed to calculate vehicle brand repair statistics", e);
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
            logger.error("Failed to calculate skill type repair statistics", e);
        }
        
        return statistics;
    }
}