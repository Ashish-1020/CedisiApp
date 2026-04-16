package com.example.ragApp.controller;

import com.example.ragApp.dto.PushTokenActionResponse;
import com.example.ragApp.dto.PushTokenRecordResponse;
import com.example.ragApp.dto.RegisterPushTokenRequest;
import com.example.ragApp.dto.UnregisterPushTokenRequest;
import com.example.ragApp.service.FcmTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/push-tokens")
public class PushTokenController {

    private final FcmTokenService fcmTokenService;

    public PushTokenController(FcmTokenService fcmTokenService) {
        this.fcmTokenService = fcmTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<PushTokenActionResponse> registerToken(@RequestBody RegisterPushTokenRequest request) {
        return ResponseEntity.ok(fcmTokenService.registerToken(request));
    }

    @PostMapping("/unregister")
    public ResponseEntity<PushTokenActionResponse> unregisterToken(@RequestBody UnregisterPushTokenRequest request) {
        return ResponseEntity.ok(fcmTokenService.unregisterToken(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PushTokenRecordResponse>> listUserTokens(@PathVariable String userId) {
        return ResponseEntity.ok(fcmTokenService.listUserTokens(userId));
    }
}

