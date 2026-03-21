package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.GreenJourneyNodeState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GreenJourneyNodeStateRepository extends JpaRepository<GreenJourneyNodeState, Long> {

    List<GreenJourneyNodeState> findByUserIdOrderByCityIndexAsc(Long userId);

    Optional<GreenJourneyNodeState> findByUserIdAndCityIndex(Long userId, Integer cityIndex);
}
