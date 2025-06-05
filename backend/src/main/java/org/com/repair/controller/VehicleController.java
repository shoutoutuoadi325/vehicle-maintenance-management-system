package org.com.repair.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.com.repair.DTO.NewVehicleRequest;
import org.com.repair.DTO.VehicleResponse;
import org.com.repair.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    
    private static final Logger logger = LoggerFactory.getLogger(VehicleController.class);
    private final VehicleService vehicleService;
    
    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Object> getVehiclesByUserId(@PathVariable Long userId) {
        try {
            logger.info("Fetching vehicles for user ID: {}", userId);
            List<VehicleResponse> vehicles = vehicleService.getVehiclesByUserId(userId);
            logger.info("Found {} vehicles for user ID: {}", vehicles.size(), userId);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            logger.error("Error fetching vehicles for user ID: " + userId, e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping
    public ResponseEntity<Object> addVehicle(@RequestBody NewVehicleRequest request) {
        try {
            logger.info("Adding new vehicle with license plate: {}", request.licensePlate());
            VehicleResponse response = vehicleService.addVehicle(request);
            logger.info("Successfully added vehicle with ID: {}", response.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error adding vehicle", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Object> getVehicleById(@PathVariable Long id) {
        try {
            logger.info("Fetching vehicle with ID: {}", id);
            return vehicleService.getVehicleById(id)
                    .map(vehicle -> ResponseEntity.ok().body((Object)vehicle))
                    .orElseGet(() -> {
                        Map<String, String> error = new HashMap<>();
                        error.put("message", "Vehicle not found");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    });
        } catch (Exception e) {
            logger.error("Error fetching vehicle with ID: " + id, e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/license-plate/{licensePlate}")
    public ResponseEntity<VehicleResponse> getVehicleByLicensePlate(@PathVariable String licensePlate) {
        return vehicleService.getVehicleByLicensePlate(licensePlate)
                .map(vehicle -> new ResponseEntity<>(vehicle, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        List<VehicleResponse> vehicles = vehicleService.getAllVehicles();
        return new ResponseEntity<>(vehicles, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateVehicle(@PathVariable Long id, @RequestBody NewVehicleRequest request) {
        try {
            logger.info("Updating vehicle with ID: {}", id);
            VehicleResponse response = vehicleService.updateVehicle(id, request);
            logger.info("Successfully updated vehicle with ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating vehicle with ID: " + id, e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteVehicle(@PathVariable Long id) {
        try {
            logger.info("Deleting vehicle with ID: {}", id);
            boolean deleted = vehicleService.deleteVehicle(id);
            if (deleted) {
                logger.info("Successfully deleted vehicle with ID: {}", id);
                return ResponseEntity.noContent().build();
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Vehicle not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
        } catch (Exception e) {
            logger.error("Error deleting vehicle with ID: " + id, e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/statistics/by-model")
    public ResponseEntity<List<Object[]>> getRepairStatisticsByModel() {
        List<Object[]> statistics = vehicleService.getRepairStatisticsByModel();
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }
} 