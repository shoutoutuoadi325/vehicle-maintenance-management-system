package org.com.repair.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.com.repair.DTO.NewVehicleRequest;
import org.com.repair.DTO.VehicleResponse;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.User;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.UserRepository;
import org.com.repair.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {
    
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final RepairOrderRepository repairOrderRepository;
    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);
    
    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository, RepairOrderRepository repairOrderRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.repairOrderRepository = repairOrderRepository;
    }
    
    @Transactional
    public VehicleResponse addVehicle(NewVehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.licensePlate())) {
            throw new RuntimeException("车牌号已存在");
        }
        
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.licensePlate());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setYear(request.year());
        vehicle.setColor(request.color());
        vehicle.setVin(request.vin());
        vehicle.setUser(user);
        
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return new VehicleResponse(savedVehicle);
    }
    
    public Optional<VehicleResponse> getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .map(VehicleResponse::new);
    }
    
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByUserId(Long userId) {
        try {
            logger.info("开始获取用户ID: {} 的车辆列表", userId);
            List<Vehicle> vehicles = vehicleRepository.findByUserId(userId);
            logger.info("成功获取到 {} 辆车", vehicles.size());
            
            List<VehicleResponse> responses = vehicles.stream()
                    .map(vehicle -> {
                        try {
                            // 为每辆车单独查询维修订单
                            List<RepairOrder> repairOrders = repairOrderRepository.findByVehicleId(vehicle.getId());
                            logger.info("车辆 {} 有 {} 个维修订单", vehicle.getLicensePlate(), repairOrders.size());
                            return new VehicleResponse(vehicle, repairOrders);
                        } catch (Exception e) {
                            logger.error("转换车辆数据时出错，车辆ID: " + vehicle.getId(), e);
                            return null;
                        }
                    })
                    .filter(response -> response != null)
                    .collect(Collectors.toList());
            
            logger.info("成功转换 {} 辆车的数据", responses.size());
            return responses;
        } catch (Exception e) {
            logger.error("获取用户车辆列表时出错，用户ID: " + userId, e);
            throw new RuntimeException("获取车辆列表失败: " + e.getMessage());
        }
    }
    
    @Transactional
    public VehicleResponse updateVehicle(Long id, NewVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("车辆不存在"));
        
        if (!vehicle.getLicensePlate().equals(request.licensePlate()) &&
            vehicleRepository.existsByLicensePlate(request.licensePlate())) {
            throw new RuntimeException("车牌号已存在");
        }
        
        if (!vehicle.getUser().getId().equals(request.userId())) {
            User newUser = userRepository.findById(request.userId())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            vehicle.setUser(newUser);
        }
        
        vehicle.setLicensePlate(request.licensePlate());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setYear(request.year());
        vehicle.setColor(request.color());
        vehicle.setVin(request.vin());
        
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return new VehicleResponse(updatedVehicle);
    }
    
    @Transactional
    public boolean deleteVehicle(Long id) {
        if (vehicleRepository.existsById(id)) {
            vehicleRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(VehicleResponse::new)
                .collect(Collectors.toList());
    }
    
    public Optional<VehicleResponse> getVehicleByLicensePlate(String licensePlate) {
        return vehicleRepository.findByLicensePlate(licensePlate)
                .map(VehicleResponse::new);
    }
    
    public List<Object[]> getRepairStatisticsByModel() {
        return vehicleRepository.getRepairStatisticsByModel();
    }
} 