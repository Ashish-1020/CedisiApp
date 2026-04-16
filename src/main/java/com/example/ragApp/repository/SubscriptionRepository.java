package com.example.ragApp.repository;

import com.example.ragApp.data.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Subscription> findTopByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Subscription> findTopByUserIdAndPlanIdOrderByCreatedAtDesc(String userId, String planId);

    List<Subscription> findByStatusIgnoreCaseAndEndDateLessThanEqualAndEndDateGreaterThanAndReminderSentAtIsNull(
            String status,
            LocalDateTime reminderBefore,
            LocalDateTime now
    );

    List<Subscription> findByStatusIgnoreCaseAndEndDateLessThanEqualAndExpiredEmailSentAtIsNull(
            String status,
            LocalDateTime now
    );
}

