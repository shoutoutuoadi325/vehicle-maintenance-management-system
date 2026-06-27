package org.com.repair.repository;

import java.util.Optional;

import org.com.repair.entity.DispatchWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DispatchWeightConfigRepository extends JpaRepository<DispatchWeightConfig, Long> {

    Optional<DispatchWeightConfig> findByConfigKeyAndEnabledTrue(String configKey);

    Optional<DispatchWeightConfig> findByConfigKey(String configKey);
}
