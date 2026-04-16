package com.example.ragApp.service;

import com.example.ragApp.data.Subscription;
import com.example.ragApp.data.User;
import com.example.ragApp.repository.SubscriptionRepository;
import com.example.ragApp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionNotificationScheduler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${app.subscription.notification.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${app.subscription.reminder-days-before:3}")
    private int reminderDaysBefore;

    public SubscriptionNotificationScheduler(SubscriptionRepository subscriptionRepository,
                                             UserRepository userRepository,
                                             MailService mailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Scheduled(fixedDelayString = "${app.subscription.notification.fixed-delay-ms:3600000}")
    @Transactional
    public void processSubscriptionNotifications() {
        if (!notificationsEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        sendExpiryReminders(now);
        markExpiredAndNotify(now);
    }

    private void sendExpiryReminders(LocalDateTime now) {
        LocalDateTime reminderBefore = now.plusDays(Math.max(reminderDaysBefore, 1));

        List<Subscription> reminderCandidates = subscriptionRepository
                .findByStatusIgnoreCaseAndEndDateLessThanEqualAndEndDateGreaterThanAndReminderSentAtIsNull(
                        "ACTIVE",
                        reminderBefore,
                        now
                );

        for (Subscription subscription : reminderCandidates) {
            userRepository.findById(subscription.getUserId()).ifPresent(user -> {
                try {
                    mailService.sendSubscriptionExpiryReminderEmail(user.getEmail(), subscription.getEndDate());
                    subscription.setReminderSentAt(now);
                    subscriptionRepository.save(subscription);
                } catch (Exception ex) {
                    log.warn("Failed expiry reminder email for subscriptionId={} userId={}: {}",
                            subscription.getId(), user.getId(), ex.getMessage());
                }
            });
        }
    }

    private void markExpiredAndNotify(LocalDateTime now) {
        List<Subscription> expiryCandidates = subscriptionRepository
                .findByStatusIgnoreCaseAndEndDateLessThanEqualAndExpiredEmailSentAtIsNull("ACTIVE", now);

        for (Subscription subscription : expiryCandidates) {
            subscription.setStatus("EXPIRED");
            subscription.setExpiredEmailSentAt(now);
            subscriptionRepository.save(subscription);

            userRepository.findById(subscription.getUserId()).ifPresent(user -> {
                applyExpiredStateToUser(user, subscription);
                try {
                    mailService.sendSubscriptionExpiredEmail(user.getEmail());
                } catch (Exception ex) {
                    log.warn("Failed expiry email for subscriptionId={} userId={}: {}",
                            subscription.getId(), user.getId(), ex.getMessage());
                }
            });
        }
    }

    private void applyExpiredStateToUser(User user, Subscription subscription) {
        user.setSubscriptionStatus("EXPIRED");
        user.setSubscriptionPlanId(subscription.getPlanId());
        user.setSubscriptionStart(subscription.getStartDate());
        user.setSubscriptionEnd(subscription.getEndDate());
        userRepository.save(user);
    }
}

