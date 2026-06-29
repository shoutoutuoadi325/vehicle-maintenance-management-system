package org.com.repair.repository;

import org.com.repair.entity.TechnicianCopilotMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TechnicianCopilotMemoryRepository extends JpaRepository<TechnicianCopilotMemory, Long> {

    Optional<TechnicianCopilotMemory> findByTechnicianId(Long technicianId);
}
