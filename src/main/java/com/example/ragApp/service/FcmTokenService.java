package com.example.ragApp.service;

import com.example.ragApp.data.FcmDeviceToken;
import com.example.ragApp.dto.PushTokenActionResponse;
import com.example.ragApp.dto.PushTokenRecordResponse;
import com.example.ragApp.dto.RegisterPushTokenRequest;
import com.example.ragApp.dto.UnregisterPushTokenRequest;
import com.example.ragApp.exception.PushTokenException;
import com.example.ragApp.repository.FcmDeviceTokenRepository;
import com.example.ragApp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class FcmTokenService {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final UserRepository userRepository;

    public FcmTokenService(FcmDeviceTokenRepository fcmDeviceTokenRepository,
                           UserRepository userRepository) {
        this.fcmDeviceTokenRepository = fcmDeviceTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PushTokenActionResponse registerToken(RegisterPushTokenRequest request) {
        validateRegisterRequest(request);

        String userId = request.getUserId().trim();
        String fcmToken = request.getFcmToken().trim();
        String deviceId = normalize(request.getDeviceId());

        if (!userRepository.existsById(userId)) {
            throw new PushTokenException("PUSH_TOKEN_USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND);
        }

        if (deviceId != null) {
            fcmDeviceTokenRepository.findByUserIdAndDeviceIdAndIsActiveTrue(userId, deviceId)
                    .filter(existing -> !fcmToken.equals(existing.getFcmToken()))
                    .ifPresent(existing -> {
                        existing.setIsActive(false);
                        existing.setLastFailureReason("Replaced by a newer token for same device");
                        existing.setUpdatedAt(LocalDateTime.now());
                        fcmDeviceTokenRepository.save(existing);
                    });
        }

        FcmDeviceToken token = fcmDeviceTokenRepository.findByFcmToken(fcmToken)
                .orElseGet(FcmDeviceToken::new);

        token.setUserId(userId);
        token.setFcmToken(fcmToken);
        token.setDeviceId(deviceId);
        token.setPlatform(normalizePlatform(request.getPlatform()));
        token.setAppVersion(normalize(request.getAppVersion()));
        token.setIsActive(true);
        token.setLastSeenAt(LocalDateTime.now());
        token.setFailureCount(0);
        token.setLastFailureReason(null);

        FcmDeviceToken saved = fcmDeviceTokenRepository.save(token);

        return new PushTokenActionResponse(
                "PUSH_TOKEN_REGISTERED",
                HttpStatus.OK.value(),
                "Push token registered",
                toRecordResponse(saved)
        );
    }

    @Transactional
    public PushTokenActionResponse unregisterToken(UnregisterPushTokenRequest request) {
        validateUnregisterRequest(request);

        String userId = request.getUserId().trim();
        String fcmToken = normalize(request.getFcmToken());
        String deviceId = normalize(request.getDeviceId());

        if (!userRepository.existsById(userId)) {
            throw new PushTokenException("PUSH_TOKEN_USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND);
        }

        FcmDeviceToken token = resolveTokenForUnregister(userId, fcmToken, deviceId);
        token.setIsActive(false);
        token.setLastFailureReason("Unregistered by client logout");
        token.setUpdatedAt(LocalDateTime.now());

        FcmDeviceToken saved = fcmDeviceTokenRepository.save(token);

        return new PushTokenActionResponse(
                "PUSH_TOKEN_UNREGISTERED",
                HttpStatus.OK.value(),
                "Push token unregistered",
                toRecordResponse(saved)
        );
    }

    @Transactional(readOnly = true)
    public List<PushTokenRecordResponse> listUserTokens(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new PushTokenException("PUSH_TOKEN_USER_ID_REQUIRED", "userId is required", HttpStatus.BAD_REQUEST);
        }

        return fcmDeviceTokenRepository.findByUserIdOrderByUpdatedAtDesc(userId.trim())
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    @Transactional
    public void markTokenInvalid(String fcmToken, String reason) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            return;
        }

        fcmDeviceTokenRepository.findByFcmToken(fcmToken.trim()).ifPresent(token -> {
            token.setIsActive(false);
            token.setFailureCount((token.getFailureCount() == null ? 0 : token.getFailureCount()) + 1);
            token.setLastFailureReason(reason == null ? "Invalid token from provider" : reason.trim());
            token.setUpdatedAt(LocalDateTime.now());
            fcmDeviceTokenRepository.save(token);
        });
    }

    private void validateRegisterRequest(RegisterPushTokenRequest request) {
        if (request == null) {
            throw new PushTokenException("PUSH_TOKEN_REQUEST_REQUIRED", "Request body is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new PushTokenException("PUSH_TOKEN_USER_ID_REQUIRED", "userId is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getFcmToken() == null || request.getFcmToken().trim().isEmpty()) {
            throw new PushTokenException("PUSH_TOKEN_VALUE_REQUIRED", "fcmToken is required", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateUnregisterRequest(UnregisterPushTokenRequest request) {
        if (request == null) {
            throw new PushTokenException("PUSH_TOKEN_REQUEST_REQUIRED", "Request body is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new PushTokenException("PUSH_TOKEN_USER_ID_REQUIRED", "userId is required", HttpStatus.BAD_REQUEST);
        }
        if ((request.getFcmToken() == null || request.getFcmToken().trim().isEmpty())
                && (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty())) {
            throw new PushTokenException(
                    "PUSH_TOKEN_IDENTIFIER_REQUIRED",
                    "Either fcmToken or deviceId is required",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private FcmDeviceToken resolveTokenForUnregister(String userId, String fcmToken, String deviceId) {
        if (fcmToken != null) {
            return fcmDeviceTokenRepository.findByUserIdAndFcmToken(userId, fcmToken)
                    .orElseThrow(() -> new PushTokenException(
                            "PUSH_TOKEN_NOT_FOUND",
                            "Token not found for this user",
                            HttpStatus.NOT_FOUND
                    ));
        }

        return fcmDeviceTokenRepository.findByUserIdAndDeviceIdAndIsActiveTrue(userId, deviceId)
                .orElseThrow(() -> new PushTokenException(
                        "PUSH_TOKEN_NOT_FOUND",
                        "Active token not found for this device",
                        HttpStatus.NOT_FOUND
                ));
    }

    private PushTokenRecordResponse toRecordResponse(FcmDeviceToken token) {
        PushTokenRecordResponse response = new PushTokenRecordResponse();
        response.setId(token.getId());
        response.setUserId(token.getUserId());
        response.setFcmToken(token.getFcmToken());
        response.setDeviceId(token.getDeviceId());
        response.setPlatform(token.getPlatform());
        response.setAppVersion(token.getAppVersion());
        response.setActive(Boolean.TRUE.equals(token.getIsActive()));
        response.setLastSeenAt(token.getLastSeenAt());
        response.setUpdatedAt(token.getUpdatedAt());
        return response;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePlatform(String platform) {
        String normalized = normalize(platform);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}

