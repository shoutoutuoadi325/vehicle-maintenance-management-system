package org.com.repair.controller;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.com.repair.DTO.NewRepairOrderRequest;
import org.com.repair.DTO.RepairOrderResponse;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.RepairOrder.RepairStatus;
import org.com.repair.service.RepairOrderService;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/repair-orders")
public class RepairOrderController {
    
    private final RepairOrderService repairOrderService;
    
    public RepairOrderController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }
    
    @PostMapping
    public ResponseEntity<RepairOrderResponse> createRepairOrder(@RequestBody NewRepairOrderRequest request) {
        RepairOrderResponse response = repairOrderService.createRepairOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RepairOrderResponse> getRepairOrderById(@PathVariable Long id) {
        return repairOrderService.getRepairOrderById(id)
                .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RepairOrderResponse>> getRepairOrdersByUserId(@PathVariable Long userId) {
        List<RepairOrderResponse> orders = repairOrderService.getRepairOrdersByUserId(userId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
    
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<RepairOrderResponse>> getRepairOrdersByVehicleId(@PathVariable Long vehicleId) {
        List<RepairOrderResponse> orders = repairOrderService.getRepairOrdersByVehicleId(vehicleId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
    
    @GetMapping("/technician/{technicianId}")
    public ResponseEntity<List<RepairOrderResponse>> getRepairOrdersByTechnician(@PathVariable Long technicianId) {
        try {
            List<RepairOrderResponse> orders = repairOrderService.getRepairOrdersByTechnician(technicianId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<RepairOrderResponse>> getRepairOrdersByStatus(@PathVariable RepairStatus status) {
        List<RepairOrderResponse> orders = repairOrderService.getRepairOrdersByStatus(status);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
    
    @GetMapping("/uncompleted")
    public ResponseEntity<List<RepairOrderResponse>> getUncompletedRepairOrders() {
        List<RepairOrderResponse> orders = repairOrderService.getUncompletedRepairOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
    
    @GetMapping
    public ResponseEntity<List<RepairOrderResponse>> getAllRepairOrders() {
        List<RepairOrderResponse> orders = repairOrderService.getAllRepairOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<RepairOrderResponse> updateOrderStatus(
            @PathVariable Long id, 
            @RequestParam RepairOrder.RepairStatus status,
            @RequestParam(required = false) Double materialCost) {
        try {
            RepairOrderResponse response = repairOrderService.updateRepairOrderStatus(id, status, materialCost);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<RepairOrderResponse> updateRepairOrder(
            @PathVariable Long id, 
            @RequestBody NewRepairOrderRequest request) {
        try {
            RepairOrderResponse response = repairOrderService.updateRepairOrder(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepairOrder(@PathVariable Long id) {
        boolean deleted = repairOrderService.deleteRepairOrder(id);
        return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    @GetMapping("/analysis/quarterly")
    public ResponseEntity<Object[]> getQuarterlyCostAnalysis(
            @RequestParam int year, 
            @RequestParam int quarter) {
        Object[] analysis = repairOrderService.getQuarterlyCostAnalysis(year, quarter);
        return new ResponseEntity<>(analysis, HttpStatus.OK);
    }
    
    @GetMapping("/analysis/monthly")
    public ResponseEntity<Object[]> getMonthlyCostAnalysis(
            @RequestParam int year, 
            @RequestParam int month) {
        Object[] analysis = repairOrderService.getMonthlyCostAnalysis(year, month);
        return new ResponseEntity<>(analysis, HttpStatus.OK);
    }
    
    @GetMapping("/negative-feedback")
    public ResponseEntity<List<Object[]>> getOrdersWithNegativeFeedback(
            @RequestParam(defaultValue = "3") int maxRating) {
        List<Object[]> orders = repairOrderService.getOrdersWithNegativeFeedback(maxRating);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
    
    @GetMapping("/task-statistics")
    public ResponseEntity<List<Object[]>> getTaskStatisticsBySkillType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<Object[]> statistics = repairOrderService.getTaskStatisticsBySkillType(startDate, endDate);
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }
    
    @PutMapping("/{id}/reassign")
    public ResponseEntity<RepairOrderResponse> reassignTechnicians(
            @PathVariable Long id,
            @RequestBody Set<Long> technicianIds,
            @RequestParam(defaultValue = "true") boolean isManual) {
        try {
            RepairOrderResponse response = repairOrderService.reassignTechnicians(id, technicianIds, isManual);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/{id}/auto-reassign")
    public ResponseEntity<RepairOrderResponse> autoReassignTechnicians(@PathVariable Long id) {
        try {
            RepairOrderResponse response = repairOrderService.autoReassignTechnicians(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}