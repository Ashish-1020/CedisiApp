package com.example.ragApp.repository;

import com.example.ragApp.data.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByPlanCodeIgnoreCase(String planCode);

    boolean existsByPlanCodeIgnoreCase(String planCode);

    List<SubscriptionPlan> findByActiveTrueOrderByCostAsc();
}

