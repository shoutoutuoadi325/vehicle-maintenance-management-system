package org.com.repair.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.com.repair.DTO.AdminResponse;
import org.com.repair.DTO.NewAdminRequest;
import org.com.repair.service.AdminService;
import org.com.repair.service.RepairOrderService;
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
@RequestMapping("/api/admins")
public class AdminController {
    
    private final AdminService adminService;
    private final RepairOrderService repairOrderService;
    private final TechnicianService technicianService;
    
    public AdminController(AdminService adminService, RepairOrderService repairOrderService, TechnicianService technicianService) {
        this.adminService = adminService;
        this.repairOrderService = repairOrderService;
        this.technicianService = technicianService;
    }
    
    @PostMapping
    public ResponseEntity<AdminResponse> registerAdmin(@RequestBody NewAdminRequest request) {
        AdminResponse response = adminService.registerAdmin(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AdminResponse> getAdminById(@PathVariable Long id) {
        return adminService.getAdminById(id)
                .map(admin -> new ResponseEntity<>(admin, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/by-username/{username}")
    public ResponseEntity<AdminResponse> getAdminByUsername(@PathVariable String username) {
        return adminService.getAdminByUsername(username)
                .map(admin -> new ResponseEntity<>(admin, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/by-role/{role}")
    public ResponseEntity<AdminResponse> getAdminByRole(@PathVariable String role) {
        return adminService.getAdminByRole(role)
                .map(admin -> new ResponseEntity<>(admin, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping
    public ResponseEntity<List<AdminResponse>> getAllAdmins() {
        List<AdminResponse> admins = adminService.getAllAdmins();
        return new ResponseEntity<>(admins, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAdmin(
            @PathVariable Long id, 
            @RequestBody NewAdminRequest request) {
        try {
            AdminResponse response = adminService.updateAdmin(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            // 返回详细的错误信息
            return new ResponseEntity<>(
                new ErrorResponse(e.getMessage()), 
                HttpStatus.BAD_REQUEST
            );
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        boolean deleted = adminService.deleteAdmin(id);
        return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AdminResponse> login(
            @RequestParam String username, 
            @RequestParam String password) {
        return adminService.login(username, password)
                .map(admin -> new ResponseEntity<>(admin, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
    }
    
    // 管理员专用端点
    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取真实的统计数据，使用带详情的查询避免懒加载问题
            List<org.com.repair.DTO.RepairOrderResponse> allOrders = repairOrderService.getAllRepairOrdersWithDetails();
            List<org.com.repair.DTO.TechnicianResponse> allTechnicians = technicianService.getAllTechnicians();
            
            long totalOrders = allOrders.size();
            long pendingOrders = allOrders.stream()
                .filter(order -> org.com.repair.entity.RepairOrder.RepairStatus.PENDING.equals(order.status()))
                .count();
            long completedOrders = allOrders.stream()
                .filter(order -> org.com.repair.entity.RepairOrder.RepairStatus.COMPLETED.equals(order.status()))
                .count();
            long activeTechnicians = allTechnicians.size();
            
            stats.put("totalOrders", totalOrders);
            stats.put("pendingOrders", pendingOrders);
            stats.put("completedOrders", completedOrders);
            stats.put("activeTechnicians", activeTechnicians);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            // 返回默认值避免前端错误
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("totalOrders", 0);
            defaultStats.put("pendingOrders", 0);
            defaultStats.put("completedOrders", 0);
            defaultStats.put("activeTechnicians", 0);
            return ResponseEntity.ok(defaultStats);
        }
    }
    
    @GetMapping("/detailed-statistics")
    public ResponseEntity<Map<String, Object>> getDetailedStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> statistics = repairOrderService.getDetailedStatistics(startDate, endDate);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取统计数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 重新分配所有待分配的订单
     * 当系统中新增技师后，可以调用此接口重新分配之前未分配的订单
     */
    @PostMapping("/reassign-pending-orders")
    public ResponseEntity<Map<String, Object>> reassignPendingOrders() {
        try {
            List<org.com.repair.DTO.RepairOrderResponse> reassignedOrders = repairOrderService.reassignPendingOrders();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "重新分配完成");
            response.put("reassignedCount", reassignedOrders.size());
            response.put("reassignedOrders", reassignedOrders);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "重新分配失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取车辆品牌维修数量统计
     */
    @GetMapping("/vehicle-brand-statistics")
    public ResponseEntity<List<Map<String, Object>>> getVehicleBrandStatistics() {
        try {
            List<Map<String, Object>> statistics = repairOrderService.getVehicleBrandRepairStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
    
    /**
     * 获取维修工种类型统计
     */
    @GetMapping("/skill-type-statistics")
    public ResponseEntity<List<Map<String, Object>>> getSkillTypeStatistics() {
        try {
            List<Map<String, Object>> statistics = repairOrderService.getSkillTypeRepairStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
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
}