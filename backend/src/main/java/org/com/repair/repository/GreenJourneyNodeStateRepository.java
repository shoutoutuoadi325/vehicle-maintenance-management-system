package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.GreenJourneyNodeState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GreenJourneyNodeStateRepository extends JpaRepository<GreenJourneyNodeState, Long> {

    List<GreenJourneyNodeState> findByUserIdAndMapIdOrderByCityIndexAsc(Long userId, Long mapId);

    Optional<GreenJourneyNodeState> findByUserIdAndMapIdAndCityIndex(Long userId, Long mapId, Integer cityIndex);

    boolean existsByUserIdAndMapIdAndNodeState(Long userId, Long mapId, String nodeState);
}
