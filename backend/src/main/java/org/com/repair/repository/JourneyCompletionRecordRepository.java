package org.com.repair.repository;

import java.util.Optional;

import org.com.repair.entity.JourneyCompletionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JourneyCompletionRecordRepository extends JpaRepository<JourneyCompletionRecord, Long> {

    Optional<JourneyCompletionRecord> findByUserId(Long userId);
}
