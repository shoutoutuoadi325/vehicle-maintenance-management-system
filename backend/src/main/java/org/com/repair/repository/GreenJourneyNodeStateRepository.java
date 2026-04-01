package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.GreenJourneyNodeState;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface GreenJourneyNodeStateRepository extends JpaRepository<GreenJourneyNodeState, Long> {

    List<GreenJourneyNodeState> findByUserIdAndMapIdOrderByCityIndexAsc(Long userId, Long mapId);

    Optional<GreenJourneyNodeState> findByUserIdAndMapIdAndCityIndex(Long userId, Long mapId, Integer cityIndex);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GreenJourneyNodeState s where s.userId = :userId and s.mapId = :mapId and s.cityIndex = :cityIndex")
    Optional<GreenJourneyNodeState> findByUserIdAndMapIdAndCityIndexForUpdate(@Param("userId") Long userId,
                                                                               @Param("mapId") Long mapId,
                                                                               @Param("cityIndex") Integer cityIndex);

    boolean existsByUserIdAndMapIdAndNodeState(Long userId, Long mapId, String nodeState);
}
