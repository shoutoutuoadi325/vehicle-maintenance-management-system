package org.com.repair.controller;

import java.util.List;

import org.com.repair.DTO.NewTechnicianRequest;
import org.com.repair.DTO.TechnicianResponse;
import org.com.repair.entity.Technician.SkillType;
import org.com.repair.service.TechnicianService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {
    
    private final TechnicianService technicianService;
    
    public TechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }
    
    @PostMapping
    public ResponseEntity<TechnicianResponse> addTechnician(@RequestBody NewTechnicianRequest request) {
        TechnicianResponse response = technicianService.addTechnician(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TechnicianResponse> getTechnicianById(@PathVariable Long id) {
        return technicianService.getTechnicianById(id)
                .map(technician -> new ResponseEntity<>(technician, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<TechnicianResponse> getTechnicianByEmployeeId(@PathVariable String employeeId) {
        return technicianService.getTechnicianByEmployeeId(employeeId)
                .map(technician -> new ResponseEntity<>(technician, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping
    public ResponseEntity<List<TechnicianResponse>> getAllTechnicians() {
        List<TechnicianResponse> technicians = technicianService.getAllTechnicians();
        return new ResponseEntity<>(technicians, HttpStatus.OK);
    }
    
    @GetMapping("/skill/{skillType}")
    public ResponseEntity<List<TechnicianResponse>> getTechniciansBySkillType(@PathVariable SkillType skillType) {
        List<TechnicianResponse> technicians = technicianService.getTechniciansBySkillType(skillType);
        return new ResponseEntity<>(technicians, HttpStatus.OK);
    }
    
    @GetMapping("/hourly-rate")
    public ResponseEntity<List<TechnicianResponse>> getTechniciansByHourlyRateRange(
            @RequestParam Double minRate, 
            @RequestParam Double maxRate) {
        List<TechnicianResponse> technicians = technicianService.getTechniciansByHourlyRateRange(minRate, maxRate);
        return new ResponseEntity<>(technicians, HttpStatus.OK);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<TechnicianResponse>> getAvailableTechnicians(
            @RequestParam(required = false) SkillType skillType) {
        List<TechnicianResponse> technicians = technicianService.getAvailableTechnicians(skillType);
        return new ResponseEntity<>(technicians, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTechnician(
            @PathVariable Long id, 
            @RequestBody NewTechnicianRequest request) {
        try {
            TechnicianResponse response = technicianService.updateTechnician(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            // 返回详细的错误信息
            return new ResponseEntity<>(
                new ErrorResponse(e.getMessage()), 
                HttpStatus.BAD_REQUEST
            );
        }
    }
    
    // 错误响应类
    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTechnician(@PathVariable Long id) {
        try {
            boolean deleted = technicianService.deleteTechnician(id);
            return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RuntimeException e) {
            // 返回详细的错误信息
            return new ResponseEntity<>(
                new ErrorResponse(e.getMessage()), 
                HttpStatus.BAD_REQUEST
            );
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<TechnicianResponse> login(
            @RequestParam String username, 
            @RequestParam String password) {
        return technicianService.login(username, password)
                .map(technician -> new ResponseEntity<>(technician, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
    }
    
    @GetMapping("/statistics/by-skill-type")
    public ResponseEntity<List<Object[]>> countTechniciansBySkillType() {
        List<Object[]> statistics = technicianService.countTechniciansBySkillType();
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }
    
    @GetMapping("/{id}/earnings")
    public ResponseEntity<Double> calculateTechnicianTotalEarnings(@PathVariable Long id) {
        Double earnings = technicianService.calculateTechnicianTotalEarnings(id);
        return earnings != null 
            ? new ResponseEntity<>(earnings, HttpStatus.OK) 
            : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    @GetMapping("/{id}/statistics")
    public ResponseEntity<TechnicianStatistics> getTechnicianStatistics(@PathVariable Long id) {
        TechnicianStatistics statistics = technicianService.getTechnicianStatistics(id);
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }
    
    @GetMapping("/{id}/monthly-earnings")
    public ResponseEntity<Double> getTechnicianMonthlyEarnings(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Double earnings = technicianService.getTechnicianMonthlyEarnings(id, year, month);
        return new ResponseEntity<>(earnings, HttpStatus.OK);
    }
    
    // 技师统计信息类
    public static class TechnicianStatistics {
        private int totalTasks;
        private int completedTasks;
        private int pendingTasks;
        private double averageRating;
        private double totalEarnings;
        private double monthlyEarnings;
        
        public TechnicianStatistics() {}
        
        public TechnicianStatistics(int totalTasks, int completedTasks, int pendingTasks, 
                                  double averageRating, double totalEarnings, double monthlyEarnings) {
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.pendingTasks = pendingTasks;
            this.averageRating = averageRating;
            this.totalEarnings = totalEarnings;
            this.monthlyEarnings = monthlyEarnings;
        }
        
        // Getters and Setters
        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
        
        public int getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }
        
        public int getPendingTasks() { return pendingTasks; }
        public void setPendingTasks(int pendingTasks) { this.pendingTasks = pendingTasks; }
        
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
        
        public double getTotalEarnings() { return totalEarnings; }
        public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }
        
        public double getMonthlyEarnings() { return monthlyEarnings; }
        public void setMonthlyEarnings(double monthlyEarnings) { this.monthlyEarnings = monthlyEarnings; }
    }
} 