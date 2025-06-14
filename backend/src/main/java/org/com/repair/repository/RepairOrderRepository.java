package org.com.repair.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.RepairOrder.RepairStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {
    
    /**
     * 根据订单号查找维修工单
     * @param orderNumber 订单号
     * @return 维修工单
     */
    Optional<RepairOrder> findByOrderNumber(String orderNumber);
    
    /**
     * 根据用户ID查找维修工单列表
     * @param userId 用户ID
     * @return 维修工单列表
     */
    List<RepairOrder> findByUserId(Long userId);
    
    /**
     * 根据用户ID查找维修工单列表（带车辆和用户信息）
     * @param userId 用户ID
     * @return 维修工单列表
     */
    @Query("SELECT r FROM RepairOrder r JOIN FETCH r.vehicle JOIN FETCH r.user WHERE r.user.id = :userId")
    List<RepairOrder> findByUserIdWithDetails(@Param("userId") Long userId);
    
    /**
     * 根据车辆ID查找维修工单列表
     * @param vehicleId 车辆ID
     * @return 维修工单列表
     */
    List<RepairOrder> findByVehicleId(Long vehicleId);
    
    /**
     * 根据维修技师ID查找维修工单列表
     * @param technicianId 维修技师ID
     * @return 维修工单列表
     */
    @Query("SELECT r FROM RepairOrder r JOIN r.technicians t WHERE t.id = :technicianId")
    List<RepairOrder> findByTechnicianId(@Param("technicianId") Long technicianId);
    
    /**
     * 根据维修技师ID和状态查找维修工单列表
     * @param technicianId 维修技师ID
     * @param status 维修状态
     * @return 维修工单列表
     */
    @Query("SELECT r FROM RepairOrder r JOIN r.technicians t WHERE t.id = :technicianId AND r.status = :status")
    List<RepairOrder> findByTechnicianIdAndStatus(@Param("technicianId") Long technicianId, @Param("status") RepairStatus status);
    
    /**
     * 根据维修状态查找维修工单列表
     * @param status 维修状态
     * @return 维修工单列表
     */
    List<RepairOrder> findByStatus(RepairStatus status);
    
    /**
     * 根据维修状态统计维修工单数量
     * @param status 维修状态
     * @return 维修工单数量
     */
    long countByStatus(RepairStatus status);
    
    /**
     * 查找指定日期范围内的维修工单列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 维修工单列表
     */
    List<RepairOrder> findByCreatedAtBetween(Date startDate, Date endDate);
    
    /**
     * 查找未完成的维修工单列表
     * @return 维修工单列表
     */
    @Query("SELECT r FROM RepairOrder r WHERE r.status != 'COMPLETED' AND r.status != 'CANCELLED'")
    List<RepairOrder> findUncompletedOrders();
    
    /**
     * 按季度或月份统计维修费用构成（工时费、材料费比例等）
     * @param year 年份
     * @param quarter 季度（1-4）
     * @return 统计结果（总工时费、总材料费、总费用）
     */
    @Query(value = "SELECT SUM(r.labor_cost) as total_labor_cost, " +
                   "SUM(r.material_cost) as total_material_cost, " +
                   "SUM(r.total_cost) as total_cost " +
                   "FROM repair_order r " +
                   "WHERE YEAR(r.created_at) = :year " +
                   "AND QUARTER(r.created_at) = :quarter",
           nativeQuery = true)
    Object[] getQuarterlyCostAnalysis(@Param("year") int year, @Param("quarter") int quarter);
    
    /**
     * 按月份统计维修费用构成
     * @param year 年份
     * @param month 月份（1-12）
     * @return 统计结果（总工时费、总材料费、总费用）
     */
    @Query(value = "SELECT SUM(r.labor_cost) as total_labor_cost, " +
                   "SUM(r.material_cost) as total_material_cost, " +
                   "SUM(r.total_cost) as total_cost " +
                   "FROM repair_order r " +
                   "WHERE YEAR(r.created_at) = :year " +
                   "AND MONTH(r.created_at) = :month",
           nativeQuery = true)
    Object[] getMonthlyCostAnalysis(@Param("year") int year, @Param("month") int month);
    
    /**
     * 获取所有有反馈的工单及涉及的员工
     * @param maxRating 参数保留以兼容现有调用，但不再使用
     * @return 统计结果（工单ID、工单号、反馈内容、技师ID、技师姓名）
     */
    @Query(value = "SELECT r.id, r.order_number, f.comment, t.id as technician_id, t.name as technician_name " +
                   "FROM repair_order r " +
                   "JOIN feedback f ON r.id = f.repair_order_id " +
                   "JOIN order_technician ot ON r.id = ot.order_id " +
                   "JOIN technician t ON ot.technician_id = t.id " +
                   "ORDER BY f.created_at DESC",
           nativeQuery = true)
    List<Object[]> findOrdersWithNegativeFeedback(@Param("maxRating") int maxRating);
    
    /**
     * 根据ID查找维修工单（包含完整的用户、车辆、技师信息）
     * @param id 工单ID
     * @return 维修工单
     */
    @Query("SELECT DISTINCT r FROM RepairOrder r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.vehicle " +
           "LEFT JOIN FETCH r.technicians " +
           "WHERE r.id = :id")
    Optional<RepairOrder> findByIdWithAllDetails(@Param("id") Long id);
    
    /**
     * 获取所有维修工单（包含用户、车辆、技师信息）
     * @return 维修工单列表
     */
    @Query("SELECT DISTINCT r FROM RepairOrder r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.vehicle " +
           "LEFT JOIN FETCH r.technicians " +
           "ORDER BY r.createdAt DESC")
    List<RepairOrder> findAllWithDetails();
    
    /**
     * 统计一段时间内，不同工种完成的任务数量及占比（基于实际工作时间）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计结果（工种、任务数量、占比、总工时）
     */
    @Query(value = "SELECT t.skill_type, COUNT(r.id) as task_count, " +
                   "COUNT(r.id) * 100.0 / (SELECT COUNT(*) FROM repair_order WHERE created_at BETWEEN :startDate AND :endDate) as percentage, " +
                   "COALESCE(SUM(TIMESTAMPDIFF(HOUR, r.created_at, r.completed_at)), 0) as total_hours " +
                   "FROM repair_order r " +
                   "JOIN order_technician ot ON r.id = ot.order_id " +
                   "JOIN technician t ON ot.technician_id = t.id " +
                   "WHERE r.created_at BETWEEN :startDate AND :endDate " +
                   "AND r.status = 'COMPLETED' " +
                   "GROUP BY t.skill_type " +
                   "ORDER BY task_count DESC",
           nativeQuery = true)
    List<Object[]> getTaskStatisticsBySkillType(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}