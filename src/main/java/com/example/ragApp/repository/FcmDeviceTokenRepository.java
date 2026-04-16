package com.example.ragApp.repository;

import com.example.ragApp.data.FcmDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, Long> {

    Optional<FcmDeviceToken> findByFcmToken(String fcmToken);

    Optional<FcmDeviceToken> findByUserIdAndFcmToken(String userId, String fcmToken);

    Optional<FcmDeviceToken> findByUserIdAndDeviceIdAndIsActiveTrue(String userId, String deviceId);

    List<FcmDeviceToken> findByUserIdOrderByUpdatedAtDesc(String userId);

    List<FcmDeviceToken> findByUserIdAndIsActiveTrue(String userId);
}

