package org.com.repair.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.com.repair.DTO.NewTechnicianRequest;
import org.com.repair.DTO.TechnicianResponse;
import org.com.repair.controller.TechnicianController.TechnicianStatistics;
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
    
    public TechnicianService(TechnicianRepository technicianRepository, 
                           RepairOrderRepository repairOrderRepository,
                           FeedbackService feedbackService) {
        this.technicianRepository = technicianRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.feedbackService = feedbackService;
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
} 