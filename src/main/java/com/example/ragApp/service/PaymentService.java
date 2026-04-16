package com.example.ragApp.service;

import com.example.ragApp.data.Payment;
import com.example.ragApp.data.Subscription;
import com.example.ragApp.data.SubscriptionPlan;
import com.example.ragApp.data.User;
import com.example.ragApp.data.Coupon;
import com.example.ragApp.dto.CreateSubscriptionRequest;
import com.example.ragApp.dto.PaymentQuoteRequest;
import com.example.ragApp.dto.PaymentQuoteResponse;
import com.example.ragApp.dto.PaymentStatusResponse;
import com.example.ragApp.dto.UpdatePaymentStatusRequest;
import com.example.ragApp.dto.VerifyPaymentRequest;
import com.example.ragApp.repository.PaymentRepository;
import com.example.ragApp.repository.SubscriptionPlanRepository;
import com.example.ragApp.repository.SubscriptionRepository;
import com.example.ragApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final int DEFAULT_DURATION_DAYS = 30;
    private static final String FREE_PLAN_ID = "FREE";

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final CouponService couponService;
    private final MailService mailService;
    private final RestClient restClient;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;

    public PaymentService(PaymentRepository paymentRepository,
                          SubscriptionRepository subscriptionRepository,
                          UserRepository userRepository,
                          SubscriptionPlanService subscriptionPlanService,
                          SubscriptionPlanRepository subscriptionPlanRepository,
                          CouponService couponService,
                          MailService mailService,
                          RestClient.Builder restClientBuilder,
                          @Value("${razorpay.key-id:}") String razorpayKeyId,
                          @Value("${razorpay.key-secret:}") String razorpayKeySecret) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.subscriptionPlanService = subscriptionPlanService;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.couponService = couponService;
        this.mailService = mailService;
        this.restClient = restClientBuilder.baseUrl("https://api.razorpay.com/v1").build();
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    @Transactional
    public PaymentStatusResponse createSubscription(CreateSubscriptionRequest request) {
        validateCreateSubscriptionRequest(request);
        validateRazorpayConfig();

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionPlan plan = subscriptionPlanService.resolveActivePlanByCode(request.getPlanId().trim());
        Coupon coupon = couponService.resolveApplicableCoupon(request.getCouponCode(), plan);

        BigDecimal originalAmount = plan.getCost();
        BigDecimal discountAmount = couponService.calculateDiscountAmount(coupon, originalAmount);
        BigDecimal finalAmount = originalAmount.subtract(discountAmount);
        if (finalAmount.signum() <= 0) {
            throw new RuntimeException("final payable amount must be greater than zero");
        }
        if (request.getAmount() != null && request.getAmount().compareTo(finalAmount) != 0) {
            throw new RuntimeException("amount mismatch with server calculated amount");
        }

        int durationDays = plan.getExpirationDays() == null ? DEFAULT_DURATION_DAYS : plan.getExpirationDays();

        Subscription subscription = new Subscription();
        subscription.setUserId(user.getId());
        subscription.setPlanId(plan.getPlanCode());
        subscription.setStatus("PENDING");
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(durationDays));
        subscription = subscriptionRepository.save(subscription);

        Payment payment = new Payment();
        payment.setUserId(user.getId());
        payment.setSubscriptionId(subscription.getId());
        payment.setAmount(finalAmount);
        payment.setOriginalAmount(originalAmount);
        payment.setDiscountAmount(discountAmount);
        if (coupon != null) {
            payment.setCouponId(coupon.getId());
        }
        payment.setCurrency(request.getCurrency().trim().toUpperCase(Locale.ROOT));
        payment.setStatus("INITIATED");
        payment.setVerificationStatus("PENDING");
        payment.setNotes("Payment initiated for subscription plan: " + subscription.getPlanId());
        payment.setOrderId(createRazorpayOrder(payment, subscription));
        payment = paymentRepository.save(payment);

        subscription.setPaymentId(payment.getId());
        subscriptionRepository.save(subscription);

        user.setSubscriptionStatus("PENDING");
        user.setSubscriptionPlanId(subscription.getPlanId());
        userRepository.save(user);

        PaymentStatusResponse response = new PaymentStatusResponse(
                "Subscription and payment created",
                payment.getId(),
                subscription.getId(),
                payment.getStatus(),
                payment.getVerificationStatus(),
                subscription.getStatus()
        );

        response.setRazorpayOrderId(payment.getOrderId());
        response.setRazorpayKeyId(razorpayKeyId);
        response.setAmountInPaise(toPaise(payment.getAmount()));
        response.setCurrency(payment.getCurrency());
        response.setOriginalAmount(payment.getOriginalAmount());
        response.setDiscountAmount(payment.getDiscountAmount());
        response.setPayableAmount(payment.getAmount());
        response.setCouponCode(coupon == null ? null : coupon.getCode());
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentQuoteResponse createQuote(PaymentQuoteRequest request) {
        if (request == null || isBlank(request.getPlanId())) {
            throw new RuntimeException("planId is required");
        }
        if (isBlank(request.getCurrency())) {
            throw new RuntimeException("currency is required");
        }

        SubscriptionPlan plan = subscriptionPlanService.resolveActivePlanByCode(request.getPlanId().trim());
        Coupon coupon = couponService.resolveApplicableCoupon(request.getCouponCode(), plan);

        BigDecimal originalAmount = plan.getCost();
        BigDecimal discountAmount = couponService.calculateDiscountAmount(coupon, originalAmount);
        BigDecimal payableAmount = originalAmount.subtract(discountAmount);
        if (payableAmount.signum() <= 0) {
            throw new RuntimeException("final payable amount must be greater than zero");
        }

        PaymentQuoteResponse response = new PaymentQuoteResponse();
        response.setPlanId(plan.getPlanCode());
        response.setPlanName(plan.getPlanName());
        response.setCurrency(request.getCurrency().trim().toUpperCase(Locale.ROOT));
        response.setOriginalAmount(originalAmount);
        response.setDiscountAmount(discountAmount);
        response.setPayableAmount(payableAmount);
        response.setCouponCode(coupon == null ? null : coupon.getCode());
        return response;
    }

    @Transactional
    public void ensureFreeSubscription(String userId) {
        if (isBlank(userId)) {
            throw new RuntimeException("userId is required");
        }

        User user = userRepository.findById(userId.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository
                .findTopByUserIdAndPlanIdOrderByCreatedAtDesc(user.getId(), FREE_PLAN_ID)
                .orElseGet(() -> {
                    Subscription freeSubscription = new Subscription();
                    freeSubscription.setUserId(user.getId());
                    freeSubscription.setPlanId(FREE_PLAN_ID);
                    freeSubscription.setStatus("ACTIVE");
                    freeSubscription.setStartDate(LocalDateTime.now());
                    // Long-lived free period so users can upgrade later.
                    freeSubscription.setEndDate(LocalDateTime.now().plusYears(100));
                    return subscriptionRepository.save(freeSubscription);
                });

        if (!"ACTIVE".equals(subscription.getStatus())) {
            subscription.setStatus("ACTIVE");
            subscription = subscriptionRepository.save(subscription);
        }

        user.setSubscriptionStatus("FREE");
        user.setSubscriptionPlanId(FREE_PLAN_ID);
        user.setPaymentId(null);
        user.setSubscriptionStart(subscription.getStartDate());
        user.setSubscriptionEnd(subscription.getEndDate());
        userRepository.save(user);

    }

    @Transactional
    public PaymentStatusResponse verifyPayment(VerifyPaymentRequest request) {
        validateRazorpayConfig();

        if (request.getPaymentId() == null) {
            throw new RuntimeException("paymentId is required");
        }

        if (isBlank(request.getRazorpayOrderId()) || isBlank(request.getRazorpayPaymentId()) || isBlank(request.getRazorpaySignature())) {
            throw new RuntimeException("razorpayOrderId, razorpayPaymentId and razorpaySignature are required");
        }

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        boolean wasAlreadySuccess = "SUCCESS".equals(payment.getStatus())
                && "VERIFIED".equals(payment.getVerificationStatus());

        String requestOrderId = request.getRazorpayOrderId().trim();
        String requestPaymentId = request.getRazorpayPaymentId().trim();
        String requestSignature = request.getRazorpaySignature().trim();

        if (!Objects.equals(payment.getOrderId(), requestOrderId)) {
            throw new RuntimeException("Order ID mismatch for payment");
        }

        final Long currentPaymentId = payment.getId();
        paymentRepository.findByGatewayTransactionId(requestPaymentId)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(currentPaymentId)) {
                        throw new RuntimeException("razorpayPaymentId already linked to another payment");
                    }
                });

        boolean signatureValid = verifyRazorpaySignature(requestOrderId, requestPaymentId, requestSignature);

        payment.setGatewayTransactionId(requestPaymentId);
        payment.setNotes(request.getNotes());

        if (signatureValid) {
            payment.setStatus("SUCCESS");
            payment.setVerificationStatus("VERIFIED");
            payment.setPaidAt(LocalDateTime.now());
            if (!wasAlreadySuccess && payment.getCouponId() != null) {
                couponService.incrementUsageCounter(payment.getCouponId());
            }
            applyActiveSubscriptionState(payment);
        } else {
            payment.setStatus("FAILED");
            payment.setVerificationStatus("UNVERIFIED");
            applyFailedSubscriptionState(payment);
        }

        payment = paymentRepository.save(payment);
        Subscription subscription = subscriptionRepository.findById(payment.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        PaymentStatusResponse response = new PaymentStatusResponse(
                signatureValid ? "Payment verified and subscription activated" : "Invalid Razorpay signature. Payment verification failed",
                payment.getId(),
                subscription.getId(),
                payment.getStatus(),
                payment.getVerificationStatus(),
                subscription.getStatus()
        );
        populatePricingResponse(response, payment);
        return response;
    }

    @Transactional
    public PaymentStatusResponse updatePaymentStatus(Long paymentId, UpdatePaymentStatusRequest request) {
        if (paymentId == null) {
            throw new RuntimeException("paymentId is required");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        boolean wasAlreadySuccess = "SUCCESS".equals(payment.getStatus())
                && "VERIFIED".equals(payment.getVerificationStatus());

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            payment.setStatus(request.getStatus().trim().toUpperCase(Locale.ROOT));
        }

        if (request.getVerificationStatus() != null && !request.getVerificationStatus().trim().isEmpty()) {
            payment.setVerificationStatus(request.getVerificationStatus().trim().toUpperCase(Locale.ROOT));
        }

        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes().trim());
        }

        if ("SUCCESS".equals(payment.getStatus())) {
            if (!wasAlreadySuccess && payment.getCouponId() != null) {
                couponService.incrementUsageCounter(payment.getCouponId());
            }
            payment.setPaidAt(LocalDateTime.now());
            applyActiveSubscriptionState(payment);
        } else if ("FAILED".equals(payment.getStatus()) || "CANCELLED".equals(payment.getStatus())) {
            applyFailedSubscriptionState(payment);
        }

        payment = paymentRepository.save(payment);
        Subscription subscription = subscriptionRepository.findById(payment.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        PaymentStatusResponse response = new PaymentStatusResponse(
                "Payment status updated",
                payment.getId(),
                subscription.getId(),
                payment.getStatus(),
                payment.getVerificationStatus(),
                subscription.getStatus()
        );
        populatePricingResponse(response, payment);
        return response;
    }

    private void validateCreateSubscriptionRequest(CreateSubscriptionRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new RuntimeException("userId is required");
        }

        if (request.getPlanId() == null || request.getPlanId().trim().isEmpty()) {
            throw new RuntimeException("planId is required");
        }

        if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
            throw new RuntimeException("currency is required");
        }
    }

    private void populatePricingResponse(PaymentStatusResponse response, Payment payment) {
        response.setCurrency(payment.getCurrency());
        response.setAmountInPaise(toPaise(payment.getAmount()));
        response.setOriginalAmount(payment.getOriginalAmount());
        response.setDiscountAmount(payment.getDiscountAmount());
        response.setPayableAmount(payment.getAmount());
    }

    private String createRazorpayOrder(Payment payment, Subscription subscription) {
        long amountInPaise = toPaise(payment.getAmount());

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amountInPaise);
        payload.put("currency", payment.getCurrency());
        payload.put("receipt", "sub_" + subscription.getId() + "_" + System.currentTimeMillis());

        Map<String, String> notes = new HashMap<>();
        notes.put("userId", payment.getUserId());
        notes.put("subscriptionId", String.valueOf(subscription.getId()));
        notes.put("planId", subscription.getPlanId());
        payload.put("notes", notes);

        Map<?, ?> razorpayResponse = restClient.post()
                .uri("/orders")
                .headers(headers -> headers.setBasicAuth(razorpayKeyId, razorpayKeySecret))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (razorpayResponse == null || razorpayResponse.get("id") == null) {
            throw new RuntimeException("Failed to create Razorpay order");
        }

        return razorpayResponse.get("id").toString();
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String razorpaySignature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(keySpec);
            byte[] digest = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = toHex(digest);
            return MessageDigest.isEqual(
                    generatedSignature.getBytes(StandardCharsets.UTF_8),
                    razorpaySignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to verify Razorpay signature", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private long toPaise(BigDecimal amount) {
        try {
            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException ex) {
            throw new RuntimeException("amount supports up to 2 decimal places", ex);
        }
    }

    private void validateRazorpayConfig() {
        if (isBlank(razorpayKeyId) || isBlank(razorpayKeySecret)) {
            throw new RuntimeException("Razorpay credentials are not configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void applyActiveSubscriptionState(Payment payment) {
        Subscription subscription = subscriptionRepository.findById(payment.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        boolean transitionedToActive = !"ACTIVE".equalsIgnoreCase(subscription.getStatus());

        subscription.setStatus("ACTIVE");
        subscriptionRepository.save(subscription);

        User user = userRepository.findById(payment.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSubscriptionStatus("ACTIVE");
        user.setPaymentId(payment.getId());
        user.setSubscriptionPlanId(subscription.getPlanId());
        user.setSubscriptionStart(subscription.getStartDate());
        user.setSubscriptionEnd(subscription.getEndDate());
        userRepository.save(user);

        if (transitionedToActive) {
            String planName = subscriptionPlanRepository.findByPlanCodeIgnoreCase(subscription.getPlanId())
                    .map(SubscriptionPlan::getPlanName)
                    .orElse(subscription.getPlanId());
            try {
                mailService.sendSubscriptionActivatedEmail(
                        user.getEmail(),
                        planName,
                        payment.getAmount(),
                        subscription.getStartDate(),
                        subscription.getEndDate()
                );
            } catch (Exception ex) {
                log.warn("Failed to send subscription activation email for subscriptionId={} userId={}: {}",
                        subscription.getId(), user.getId(), ex.getMessage());
            }
        }
    }

    private void applyFailedSubscriptionState(Payment payment) {
        Subscription subscription = subscriptionRepository.findById(payment.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        subscription.setStatus("FAILED");
        subscriptionRepository.save(subscription);

        User user = userRepository.findById(payment.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSubscriptionStatus("FREE");
        user.setPaymentId(null);
        user.setSubscriptionStart(null);
        user.setSubscriptionEnd(null);
        userRepository.save(user);
    }
}

