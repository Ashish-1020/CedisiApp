package com.example.ragApp.repository;

import com.example.ragApp.data.PlanPromptMapping;
import com.example.ragApp.data.PlanPromptMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanPromptMappingRepository extends JpaRepository<PlanPromptMapping, PlanPromptMappingId> {

    @Query("SELECT ppm FROM PlanPromptMapping ppm WHERE UPPER(ppm.id.planCode) = UPPER(:planCode)")
    List<PlanPromptMapping> findByPlanCodeIgnoreCase(@Param("planCode") String planCode);

    @Query("SELECT ppm.id.promptId FROM PlanPromptMapping ppm WHERE UPPER(ppm.id.planCode) = UPPER(:planCode)")
    List<UUID> findPromptIdsByPlanCodeIgnoreCase(@Param("planCode") String planCode);

    boolean existsByIdPlanCodeIgnoreCaseAndIdPromptId(String planCode, UUID promptId);

    long deleteByIdPlanCodeIgnoreCaseAndIdPromptId(String planCode, UUID promptId);
}
