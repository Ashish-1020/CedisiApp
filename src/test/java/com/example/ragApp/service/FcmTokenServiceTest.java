package com.example.ragApp.service;

import com.example.ragApp.data.FcmDeviceToken;
import com.example.ragApp.dto.PushTokenActionResponse;
import com.example.ragApp.dto.RegisterPushTokenRequest;
import com.example.ragApp.dto.UnregisterPushTokenRequest;
import com.example.ragApp.exception.PushTokenException;
import com.example.ragApp.repository.FcmDeviceTokenRepository;
import com.example.ragApp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock
    private FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    @Test
    void registerTokenShouldUpsertToken() {
        RegisterPushTokenRequest request = new RegisterPushTokenRequest();
        request.setUserId("u-1");
        request.setFcmToken("fcm-token");
        request.setDeviceId("device-1");
        request.setPlatform("android");

        when(userRepository.existsById("u-1")).thenReturn(true);
        when(fcmDeviceTokenRepository.findByFcmToken("fcm-token")).thenReturn(Optional.empty());
        when(fcmDeviceTokenRepository.save(any(FcmDeviceToken.class))).thenAnswer(invocation -> {
            FcmDeviceToken token = invocation.getArgument(0);
            token.setId(10L);
            return token;
        });

        PushTokenActionResponse response = fcmTokenService.registerToken(request);

        assertEquals("PUSH_TOKEN_REGISTERED", response.getCode());
        assertEquals("ANDROID", response.getToken().getPlatform());
        assertEquals(true, response.getToken().getActive());
    }

    @Test
    void unregisterTokenShouldFailWhenUserDoesNotExist() {
        UnregisterPushTokenRequest request = new UnregisterPushTokenRequest();
        request.setUserId("missing");
        request.setFcmToken("fcm-token");

        when(userRepository.existsById("missing")).thenReturn(false);

        PushTokenException ex = assertThrows(PushTokenException.class, () -> fcmTokenService.unregisterToken(request));
        assertEquals("PUSH_TOKEN_USER_NOT_FOUND", ex.getCode());
    }
}

