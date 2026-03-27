package org.com.repair.repository;

import java.util.List;
import java.util.Optional;

import org.com.repair.entity.JourneyMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JourneyMapRepository extends JpaRepository<JourneyMap, Long> {

    List<JourneyMap> findByEnabledTrueOrderByIdAsc();

    Optional<JourneyMap> findFirstByEnabledTrueOrderByIdAsc();
}
