package com.example.ragApp.repository;

import com.example.ragApp.data.LlmModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LlmModelConfigRepository extends JpaRepository<LlmModelConfig, Long> {

    Optional<LlmModelConfig> findFirstByActiveTrueOrderByUpdatedAtDesc();

    Optional<LlmModelConfig> findFirstByLastKnownGoodTrueOrderByUpdatedAtDesc();

    List<LlmModelConfig> findAllByOrderByUpdatedAtDesc();

    boolean existsByProviderIgnoreCaseAndModelNameIgnoreCase(String provider, String modelName);

    @Modifying
    @Query("update LlmModelConfig c set c.active = false where c.active = true")
    int deactivateAllActive();

    @Modifying
    @Query("update LlmModelConfig c set c.lastKnownGood = false where c.lastKnownGood = true")
    int clearLastKnownGoodFlag();
}

