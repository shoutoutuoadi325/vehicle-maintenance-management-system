package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.Material;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Material m where m.id = :id")
    Optional<Material> findByIdForUpdate(@Param("id") Long id);
    
    /**
     * 根据材料名称查找材料列表
     * @param name 材料名称
     * @return 材料列表
     */
    List<Material> findByNameContaining(String name);
    
    /**
     * 查找特定价格范围内的材料列表
     * @param minUnitPrice 最低单价
     * @param maxUnitPrice 最高单价
     * @return 材料列表
     */
    List<Material> findByUnitPriceBetween(Double minUnitPrice, Double maxUnitPrice);
} 