package com.hotelbooking.repository;

import com.hotelbooking.model.AiConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiConfigurationRepository extends JpaRepository<AiConfiguration, String> {
    Optional<AiConfiguration> findByConfigKey(String configKey);
}

