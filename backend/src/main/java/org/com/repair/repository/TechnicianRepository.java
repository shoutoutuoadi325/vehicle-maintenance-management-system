package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.Technician;
import org.com.repair.entity.Technician.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {
    
    /**
     * 根据员工ID查找技师
     * @param employeeId 员工ID
     * @return 技师信息
     */
    Optional<Technician> findByEmployeeId(String employeeId);
    
    /**
     * 根据用户名查找技师
     * @param username 用户名
     * @return 技师信息
     */
    Optional<Technician> findByUsername(String username);
    
    /**
     * 根据用户名和密码查找技师（用于登录验证）
     * @param username 用户名
     * @param password 密码
     * @return 技师信息
     */
    Optional<Technician> findByUsernameAndPassword(String username, String password);
    
    /**
     * 根据技能类型查找技师列表
     * @param skillType 技能类型
     * @return 技师列表
     */
    List<Technician> findBySkillType(SkillType skillType);
    
    /**
     * 查找特定时薪范围内的技师列表
     * @param minHourlyRate 最低时薪
     * @param maxHourlyRate 最高时薪
     * @return 技师列表
     */
    List<Technician> findByHourlyRateBetween(Double minHourlyRate, Double maxHourlyRate);
    
    /**
     * 根据技能类型和时薪范围查找技师列表
     * @param skillType 技能类型
     * @param minHourlyRate 最低时薪
     * @param maxHourlyRate 最高时薪
     * @return 技师列表
     */
    List<Technician> findBySkillTypeAndHourlyRateBetween(SkillType skillType, Double minHourlyRate, Double maxHourlyRate);
    
    /**
     * 检查员工ID是否已存在
     * @param employeeId 员工ID
     * @return 是否存在
     */
    boolean existsByEmployeeId(String employeeId);
    
    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 统计各技能类型的技师数量
     * @return 统计结果（技能类型、技师数量）
     */
    @Query("SELECT t.skillType, COUNT(t) FROM Technician t GROUP BY t.skillType")
    List<Object[]> countTechniciansBySkillType();
    
    /**
     * 计算技师的历史工时费总收入（基于实际工作时间）
     * @param technicianId 技师ID
     * @return 总收入
     */
    @Query(value = "SELECT COALESCE(SUM(r.actual_hours * t.hourly_rate), 0) " +
                   "FROM repair_order r " +
                   "JOIN order_technician ot ON r.id = ot.order_id " +
                   "JOIN technician t ON ot.technician_id = t.id " +
                   "WHERE ot.technician_id = :technicianId " +
                   "AND r.status = 'COMPLETED' " +
                   "AND r.actual_hours IS NOT NULL",
           nativeQuery = true)
    Double calculateTotalEarnings(@Param("technicianId") Long technicianId);
    
    /**
     * 计算技师指定月份的收入（基于实际工作时间）
     * @param technicianId 技师ID
     * @param year 年份
     * @param month 月份
     * @return 月收入
     */
    @Query(value = "SELECT COALESCE(SUM(r.actual_hours * t.hourly_rate), 0) " +
                   "FROM repair_order r " +
                   "JOIN order_technician ot ON r.id = ot.order_id " +
                   "JOIN technician t ON ot.technician_id = t.id " +
                   "WHERE ot.technician_id = :technicianId " +
                   "AND r.status = 'COMPLETED' " +
                   "AND r.actual_hours IS NOT NULL " +
                   "AND YEAR(r.completed_at) = :year " +
                   "AND MONTH(r.completed_at) = :month",
           nativeQuery = true)
    Double calculateMonthlyEarnings(@Param("technicianId") Long technicianId, 
                                   @Param("year") int year, 
                                   @Param("month") int month);
    
    /**
     * 查找指定时间段内可用的技师列表（按工作负载排序）
     * @param skillType 技能类型
     * @return 可用技师列表
     */
    @Query("SELECT t FROM Technician t " +
           "WHERE t.skillType = :skillType " +
           "ORDER BY " +
           "(SELECT COUNT(r) FROM t.repairOrders r WHERE r.status != 'COMPLETED' AND r.status != 'CANCELLED'), " +
           "t.hourlyRate, " +
           "t.completedOrders DESC")
    List<Technician> findAvailableTechnicians(@Param("skillType") SkillType skillType);
    
    /**
     * 删除技师前先清除与订单的关联关系
     * @param technicianId 技师ID
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM order_technician WHERE technician_id = :technicianId", nativeQuery = true)
    void removeFromAllOrders(@Param("technicianId") Long technicianId);
}