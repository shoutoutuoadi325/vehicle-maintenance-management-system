package org.com.repair.repository;

import java.util.Date;
import java.util.List;

import org.com.repair.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    /**
     * 根据维修工单ID查找反馈列表
     * @param repairOrderId 维修工单ID
     * @return 反馈列表
     */
    List<Feedback> findByRepairOrderId(Long repairOrderId);
    
    /**
     * 根据用户ID查找反馈列表
     * @param userId 用户ID
     * @return 反馈列表
     */
    List<Feedback> findByUserId(Long userId);
    
    /**
     * 检查特定用户对特定维修工单是否已经有反馈
     * @param repairOrderId 维修工单ID
     * @param userId 用户ID
     * @return 反馈列表
     */
    List<Feedback> findByRepairOrderIdAndUserId(Long repairOrderId, Long userId);
    
    /**
     * 检查特定用户对特定维修工单是否已经有反馈（返回是否存在）
     * @param repairOrderId 维修工单ID
     * @param userId 用户ID
     * @return 是否存在反馈
     */
    boolean existsByRepairOrderIdAndUserId(Long repairOrderId, Long userId);
    
    /**
     * 查找指定日期范围内的反馈列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 反馈列表
     */
    List<Feedback> findByCreatedAtBetween(Date startDate, Date endDate);
    
    /**
     * 查找包含特定关键词的反馈列表
     * @param keyword 关键词
     * @return 反馈列表
     */
    List<Feedback> findByCommentContaining(String keyword);
    
    /**
     * 计算指定技师的平均评分
     * @param technicianId 技师ID
     * @return 平均评分
     */
    @Query("SELECT AVG(f.rating) FROM Feedback f " +
           "JOIN f.repairOrder r " +
           "JOIN r.technicians t " +
           "WHERE t.id = :technicianId " +
           "AND f.rating IS NOT NULL")
    Double getAverageRatingByTechnicianId(@Param("technicianId") Long technicianId);

} 