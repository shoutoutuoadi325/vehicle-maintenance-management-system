package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.JourneyMapNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JourneyMapNodeRepository extends JpaRepository<JourneyMapNode, Long> {

    List<JourneyMapNode> findByMapIdOrderByCityIndexAsc(Long mapId);

    Optional<JourneyMapNode> findByMapIdAndCityIndex(Long mapId, Integer cityIndex);
}
