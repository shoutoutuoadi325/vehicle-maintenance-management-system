package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    
    /**
     * 根据用户ID查找车辆列表
     * @param userId 用户ID
     * @return 车辆列表
     */
    List<Vehicle> findByUserId(Long userId);
    
    /**
     * 根据车牌号查找车辆
     * @param licensePlate 车牌号
     * @return 车辆信息
     */
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    
    /**
     * 检查车牌号是否已存在
     * @param licensePlate 车牌号
     * @return 是否存在
     */
    boolean existsByLicensePlate(String licensePlate);
    
    /**
     * 根据车辆品牌查找车辆列表
     * @param brand 品牌
     * @return 车辆列表
     */
    List<Vehicle> findByBrand(String brand);
    
    /**
     * 根据车辆型号查找车辆列表
     * @param model 型号
     * @return 车辆列表
     */
    List<Vehicle> findByModel(String model);
    
    /**
     * 根据品牌和型号查找车辆列表
     * @param brand 品牌
     * @param model 型号
     * @return 车辆列表
     */
    List<Vehicle> findByBrandAndModel(String brand, String model);
    
    /**
     * 统计各车型的维修次数与平均维修费用
     * @return 统计结果（车型、维修次数、平均维修费用）
     */
    @Query(value = "SELECT v.brand, v.model, COUNT(r.id) as repair_count, AVG(r.total_cost) as avg_cost " +
                   "FROM Vehicle v " +
                   "JOIN RepairOrder r ON v.id = r.vehicle_id " +
                   "GROUP BY v.brand, v.model " +
                   "ORDER BY repair_count DESC", 
           nativeQuery = true)
    List<Object[]> getRepairStatisticsByModel();
    
    /**
     * 查询特定车型最常出现的故障类型
     * @param brand 品牌
     * @param model 型号
     * @return 统计结果（故障描述、出现次数）
     */
    @Query(value = "SELECT r.description, COUNT(r.id) as occurrence_count " +
                   "FROM RepairOrder r " +
                   "JOIN Vehicle v ON r.vehicle_id = v.id " +
                   "WHERE v.brand = :brand AND v.model = :model " +
                   "GROUP BY r.description " +
                   "ORDER BY occurrence_count DESC",
           nativeQuery = true)
    List<Object[]> findMostCommonIssuesByModel(@Param("brand") String brand, @Param("model") String model);
    
    /**
     * 根据用户ID查找车辆列表（带用户和维修订单信息）
     * @param userId 用户ID
     * @return 车辆列表
     */
    @Query("SELECT DISTINCT v FROM Vehicle v LEFT JOIN FETCH v.user LEFT JOIN FETCH v.repairOrders WHERE v.user.id = :userId")
    List<Vehicle> findByUserIdWithDetails(@Param("userId") Long userId);
} 