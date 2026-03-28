package org.com.repair.repository;

import org.com.repair.entity.JourneyFootprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JourneyFootprintRepository extends JpaRepository<JourneyFootprint, Long> {

    Page<JourneyFootprint> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
