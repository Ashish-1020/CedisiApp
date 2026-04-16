package com.example.ragApp.controller;

import com.example.ragApp.dto.CreateSubscriptionRequest;
import com.example.ragApp.dto.PaymentQuoteRequest;
import com.example.ragApp.dto.PaymentQuoteResponse;
import com.example.ragApp.dto.PaymentStatusResponse;
import com.example.ragApp.dto.UpdatePaymentStatusRequest;
import com.example.ragApp.dto.VerifyPaymentRequest;
import com.example.ragApp.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<PaymentStatusResponse> createSubscription(@RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.ok(paymentService.createSubscription(request));
    }

    @PostMapping("/quote")
    public ResponseEntity<PaymentQuoteResponse> createQuote(@RequestBody PaymentQuoteRequest request) {
        return ResponseEntity.ok(paymentService.createQuote(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentStatusResponse> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    @PutMapping("/{paymentId}/status")
    public ResponseEntity<PaymentStatusResponse> updatePaymentStatus(@PathVariable Long paymentId,
                                                                     @RequestBody UpdatePaymentStatusRequest request) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(paymentId, request));
    }
}

