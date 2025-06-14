package org.com.repair.service;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.com.repair.DTO.NewTechnicianRequest;
import org.com.repair.DTO.RepairOrderResponse;
import org.com.repair.DTO.TechnicianResponse;
import org.com.repair.controller.TechnicianController.TechnicianStatistics;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Technician;
import org.com.repair.entity.Technician.SkillType;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.TechnicianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TechnicianService {
    
    private final TechnicianRepository technicianRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final FeedbackService feedbackService;
    private final AutoAssignmentService autoAssignmentService;
    
    public TechnicianService(TechnicianRepository technicianRepository, 
                           RepairOrderRepository repairOrderRepository,
                           FeedbackService feedbackService,
                           AutoAssignmentService autoAssignmentService) {
        this.technicianRepository = technicianRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.feedbackService = feedbackService;
        this.autoAssignmentService = autoAssignmentService;
    }
    
    @Transactional
    public TechnicianResponse addTechnician(NewTechnicianRequest request) {
        if (technicianRepository.existsByEmployeeId(request.employeeId())) {
            throw new RuntimeException("员工ID已存在");
        }
        
        if (technicianRepository.existsByUsername(request.username())) {
            throw new RuntimeException("用户名已存在");
        }
        
        Technician technician = new Technician();
        technician.setName(request.name());
        technician.setEmployeeId(request.employeeId());
        technician.setUsername(request.username());
        technician.setPassword(request.password());
        technician.setPhone(request.phone());
        technician.setEmail(request.email());
        technician.setSkillType(request.skillType());
        technician.setHourlyRate(request.hourlyRate());
        
        Technician savedTechnician = technicianRepository.save(technician);
        return new TechnicianResponse(savedTechnician);
    }
    
    public Optional<TechnicianResponse> getTechnicianById(Long id) {
        return technicianRepository.findById(id)
                .map(TechnicianResponse::new);
    }
    
    public Optional<TechnicianResponse> getTechnicianByEmployeeId(String employeeId) {
        return technicianRepository.findByEmployeeId(employeeId)
                .map(TechnicianResponse::new);
    }
    
    public Optional<TechnicianResponse> getTechnicianByUsername(String username) {
        return technicianRepository.findByUsername(username)
                .map(TechnicianResponse::new);
    }
    
    @Transactional
    public TechnicianResponse updateTechnician(Long id, NewTechnicianRequest request) {
        Technician technician = technicianRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("技师不存在"));
        
        // 如果员工ID更改了，检查新员工ID是否已存在
        if (!technician.getEmployeeId().equals(request.employeeId()) &&
            technicianRepository.existsByEmployeeId(request.employeeId())) {
            throw new RuntimeException("员工ID已存在");
        }
        
        // 如果用户名更改了，检查新用户名是否已存在
        if (!technician.getUsername().equals(request.username()) &&
            technicianRepository.existsByUsername(request.username())) {
            throw new RuntimeException("用户名已存在");
        }
        
        technician.setName(request.name());
        technician.setEmployeeId(request.employeeId());
        technician.setUsername(request.username());
        if (request.password() != null && !request.password().isEmpty()) {
            technician.setPassword(request.password());
        }
        technician.setPhone(request.phone());
        technician.setEmail(request.email());
        technician.setSkillType(request.skillType());
        technician.setHourlyRate(request.hourlyRate());
        
        Technician updatedTechnician = technicianRepository.save(technician);
        return new TechnicianResponse(updatedTechnician);
    }
    
    @Transactional
    public boolean deleteTechnician(Long id) {
        if (!technicianRepository.existsById(id)) {
            return false;
        }
        
        try {
            // 先清除技师与所有订单的关联关系
            technicianRepository.removeFromAllOrders(id);
            
            // 然后删除技师
            technicianRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("删除技师失败: " + e.getMessage(), e);
        }
    }
    
    public List<TechnicianResponse> getAllTechnicians() {
        return technicianRepository.findAll().stream()
                .map(TechnicianResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<TechnicianResponse> getTechniciansBySkillType(SkillType skillType) {
        return technicianRepository.findBySkillType(skillType).stream()
                .map(TechnicianResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<TechnicianResponse> getTechniciansByHourlyRateRange(Double minRate, Double maxRate) {
        return technicianRepository.findByHourlyRateBetween(minRate, maxRate).stream()
                .map(TechnicianResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<Object[]> countTechniciansBySkillType() {
        return technicianRepository.countTechniciansBySkillType();
    }
    
    public Double calculateTechnicianTotalEarnings(Long technicianId) {
        return technicianRepository.calculateTotalEarnings(technicianId);
    }
    
    public List<TechnicianResponse> getAvailableTechnicians(SkillType skillType) {
        return technicianRepository.findAvailableTechnicians(skillType).stream()
                .map(TechnicianResponse::new)
                .collect(Collectors.toList());
    }
    
    public Optional<TechnicianResponse> login(String username, String password) {
        return technicianRepository.findByUsernameAndPassword(username, password)
                .map(TechnicianResponse::new);
    }
    
    public long getActiveTechniciansCount() {
        return technicianRepository.count(); // 假设所有技师都是活跃的
    }
    
    public TechnicianStatistics getTechnicianStatistics(Long technicianId) {
        // 获取技师的所有任务
        List<org.com.repair.entity.RepairOrder> allTasks = repairOrderRepository.findByTechnicianId(technicianId);
        
        int totalTasks = allTasks.size();
        int completedTasks = (int) allTasks.stream()
            .filter(task -> "COMPLETED".equals(task.getStatus()))
            .count();
        int pendingTasks = (int) allTasks.stream()
            .filter(task -> "ASSIGNED".equals(task.getStatus()) || "IN_PROGRESS".equals(task.getStatus()))
            .count();
        
        // 获取平均评分
        Double averageRating = feedbackService.getAverageRatingByTechnicianId(technicianId);
        if (averageRating == null) {
            averageRating = 0.0;
        }
        
        // 获取总收入
        Double totalEarnings = technicianRepository.calculateTotalEarnings(technicianId);
        if (totalEarnings == null) {
            totalEarnings = 0.0;
        }
        
        // 获取本月收入
        Double monthlyEarnings = getTechnicianMonthlyEarnings(technicianId, null, null);
        
        return new TechnicianStatistics(totalTasks, completedTasks, pendingTasks, 
                                      averageRating, totalEarnings, monthlyEarnings);
    }
    
    public Double getTechnicianMonthlyEarnings(Long technicianId, Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();
        
        return technicianRepository.calculateMonthlyEarnings(technicianId, targetYear, targetMonth);
    }
    
    /**
     * 技师拒绝订单
     * @param technicianId 技师ID
     * @param orderId 订单ID
     * @param reason 拒绝原因
     * @return 是否拒绝成功
     */
    @Transactional
    public boolean rejectOrder(Long technicianId, Long orderId, String reason) {
        // 1. 验证技师是否存在
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new RuntimeException("技师不存在"));
        
        // 2. 验证订单是否存在
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("维修订单不存在"));
        
        // 3. 验证订单是否分配给该技师
        boolean isAssignedToTechnician = order.getTechnicians() != null && 
                order.getTechnicians().stream()
                        .anyMatch(t -> t.getId().equals(technicianId));
        
        if (!isAssignedToTechnician) {
            throw new RuntimeException("此订单未分配给您，无法拒绝");
        }
        
        // 4. 验证订单状态是否可以拒绝（只有ASSIGNED状态的订单可以拒绝）
        if (order.getStatus() != RepairOrder.RepairStatus.ASSIGNED) {
            throw new RuntimeException("订单状态不允许拒绝，只有已分配状态的订单可以拒绝");
        }
        
        // 5. 从订单的技师列表中移除当前技师
        order.getTechnicians().removeIf(t -> t.getId().equals(technicianId));
        
        // 6. 记录拒绝原因（可以在RepairOrder中添加拒绝记录字段，这里暂时在日志中记录）
        System.out.println(String.format("技师 %s (ID: %d) 拒绝了订单 %s (ID: %d)，原因：%s", 
                technician.getName(), technicianId, order.getOrderNumber(), orderId, 
                reason != null ? reason : "未提供原因"));
        
        // 7. 重新自动分配给其他技师
        try {
            // 获取订单所需的技能类型
            SkillType requiredSkillType = order.getRequiredSkillType();
            if (requiredSkillType == null) {
                // 如果没有指定技能类型，从描述中分析
                var skillTypes = autoAssignmentService.determineRequiredSkillTypes(order.getDescription());
                if (!skillTypes.isEmpty()) {
                    requiredSkillType = skillTypes.iterator().next(); // 取第一个
                }
            }
            
            if (requiredSkillType != null) {
                // 排除已拒绝的技师，寻找其他技师
                Technician newTechnician = autoAssignmentService.autoAssignBestTechnicianExcluding(
                        requiredSkillType, technicianId);
                
                if (newTechnician != null) {
                    // 分配给新技师
                    order.getTechnicians().add(newTechnician);
                    order.setUpdatedAt(new Date());
                    order.setStatus(RepairOrder.RepairStatus.ASSIGNED);
                    
                    repairOrderRepository.save(order);
                    
                    System.out.println(String.format("订单 %s 已重新分配给技师 %s (ID: %d)", 
                            order.getOrderNumber(), newTechnician.getName(), newTechnician.getId()));
                    
                    return true;
                } else {
                    // 没有其他可用技师，将订单状态改为PENDING
                    order.setStatus(RepairOrder.RepairStatus.PENDING);
                    order.setUpdatedAt(new Date());
                    repairOrderRepository.save(order);
                    
                    System.out.println(String.format("订单 %s 无其他可用技师，已改为待分配状态", 
                            order.getOrderNumber()));
                    
                    return true;
                }
            } else {
                throw new RuntimeException("无法确定订单所需技能类型，无法重新分配");
            }
        } catch (Exception e) {
            // 重新分配失败，将订单状态改为PENDING
            order.setStatus(RepairOrder.RepairStatus.PENDING);
            order.setUpdatedAt(new Date());
            repairOrderRepository.save(order);
            
            System.err.println(String.format("订单 %s 重新分配失败：%s，已改为待分配状态", 
                    order.getOrderNumber(), e.getMessage()));
            
            return true; // 拒绝操作成功，但重新分配失败
        }
    }
    
    /**
     * 获取分配给技师的待处理订单
     * @param technicianId 技师ID
     * @return 订单列表
     */
    public List<Object> getAssignedOrders(Long technicianId) {
        // 验证技师是否存在
        if (!technicianRepository.existsById(technicianId)) {
            throw new RuntimeException("技师不存在");
        }
        
        // 获取分配给该技师且状态为ASSIGNED的订单
        List<RepairOrder> assignedOrders = repairOrderRepository.findByTechnicianIdAndStatus(
                technicianId, RepairOrder.RepairStatus.ASSIGNED);
        
        return assignedOrders.stream()
                .map(order -> new RepairOrderResponse(order))
                .collect(Collectors.toList())
                .stream()
                .map(response -> (Object) response)
                .collect(Collectors.toList());
    }
} 