package org.com.repair.service;

import java.text.SimpleDateFormat;
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
        
        // 如果指定了技师，使用手动分配
        if (request.technicianIds() != null && !request.technicianIds().isEmpty()) {
            Set<Technician> technicians = new HashSet<>();
            for (Long technicianId : request.technicianIds()) {
                Technician technician = technicianRepository.findById(technicianId)
                        .orElseThrow(() -> new RuntimeException("技师不存在: " + technicianId));
                technicians.add(technician);
            }
            repairOrder.setTechnicians(technicians);
            repairOrder.setAssignmentType(RepairOrder.AssignmentType.MANUAL);
            repairOrder.setStatus(RepairOrder.RepairStatus.ASSIGNED);
        } else {
            // 自动分配技师
            Set<org.com.repair.entity.Technician.SkillType> requiredSkills = 
                autoAssignmentService.determineRequiredSkillTypes(request.description());
            Set<Technician> assignedTechnicians = 
                autoAssignmentService.autoAssignTechnicians(repairOrder, requiredSkills);
            
            if (!assignedTechnicians.isEmpty()) {
                repairOrder.setTechnicians(assignedTechnicians);
                repairOrder.setAssignmentType(RepairOrder.AssignmentType.AUTO);
                repairOrder.setStatus(RepairOrder.RepairStatus.ASSIGNED);
                
                // 计算预估工时
                double totalEstimatedHours = 0;
                for (org.com.repair.entity.Technician.SkillType skillType : requiredSkills) {
                    totalEstimatedHours += autoAssignmentService.estimateWorkHours(request.description(), skillType);
                }
                repairOrder.setEstimatedHours(totalEstimatedHours);
            }
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
    public RepairOrderResponse updateRepairOrderStatus(Long id, RepairStatus status) {
        RepairOrder repairOrder = repairOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("维修工单不存在"));
        
        repairOrder.setStatus(status);
        repairOrder.setUpdatedAt(new Date());
        
        if (status == RepairStatus.COMPLETED) {
            repairOrder.setCompletedAt(new Date());
        }
        
        RepairOrder updatedOrder = repairOrderRepository.save(repairOrder);
        return new RepairOrderResponse(updatedOrder);
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
    
    private String generateOrderNumber() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateStr = dateFormat.format(new Date());
        long count = repairOrderRepository.count() + 1;
        return "RO" + dateStr + String.format("%04d", count);
    }
}