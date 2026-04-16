package com.example.ragApp.service;

import com.example.ragApp.data.Coupon;
import com.example.ragApp.data.SubscriptionPlan;
import com.example.ragApp.dto.CouponRequest;
import com.example.ragApp.dto.CouponResponse;
import com.example.ragApp.repository.CouponRepository;
import com.example.ragApp.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CouponService {

    private static final String TYPE_PERCENT = "PERCENT";
    private static final String TYPE_FLAT = "FLAT";

    private final CouponRepository couponRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public CouponService(CouponRepository couponRepository,
                         SubscriptionPlanRepository subscriptionPlanRepository) {
        this.couponRepository = couponRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        validateRequest(request);

        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("coupon code already exists");
        }

        Coupon coupon = new Coupon();
        applyValues(coupon, request);
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse getCouponById(Long couponId) {
        return toResponse(getCouponEntity(couponId));
    }

    @Transactional
    public CouponResponse updateCoupon(Long couponId, CouponRequest request) {
        validateRequest(request);

        Coupon coupon = getCouponEntity(couponId);
        String incomingCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (!coupon.getCode().equalsIgnoreCase(incomingCode)
                && couponRepository.existsByCodeIgnoreCase(incomingCode)) {
            throw new RuntimeException("coupon code already exists");
        }

        applyValues(coupon, request);
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public void deactivateCoupon(Long couponId) {
        Coupon coupon = getCouponEntity(couponId);
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    @Transactional
    public CouponResponse attachPlan(Long couponId, Long planId) {
        Coupon coupon = getCouponEntity(couponId);
        SubscriptionPlan plan = getPlanEntity(planId);
        coupon.getPlans().add(plan);
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse detachPlan(Long couponId, Long planId) {
        Coupon coupon = getCouponEntity(couponId);
        coupon.getPlans().removeIf(plan -> plan.getId().equals(planId));
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional(readOnly = true)
    public Coupon resolveApplicableCoupon(String couponCode, SubscriptionPlan plan) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return null;
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new RuntimeException("Coupon is inactive");
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new RuntimeException("Coupon is not active yet");
        }
        if (coupon.getValidTill() != null && now.isAfter(coupon.getValidTill())) {
            throw new RuntimeException("Coupon has expired");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new RuntimeException("Coupon usage limit exceeded");
        }

        boolean mappedToPlan = coupon.getPlans()
                .stream()
                .anyMatch(p -> p.getId().equals(plan.getId()));
        if (!mappedToPlan) {
            throw new RuntimeException("Coupon is not valid for selected plan");
        }

        return coupon;
    }

    @Transactional
    public void incrementUsageCounter(Long couponId) {
        if (couponId == null) {
            return;
        }
        Coupon coupon = getCouponEntity(couponId);
        int currentUsedCount = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        if (coupon.getUsageLimit() != null && currentUsedCount >= coupon.getUsageLimit()) {
            throw new RuntimeException("Coupon usage limit exceeded");
        }
        coupon.setUsedCount(currentUsedCount + 1);
        couponRepository.save(coupon);
    }

    public BigDecimal calculateDiscountAmount(Coupon coupon, BigDecimal amount) {
        if (coupon == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (TYPE_PERCENT.equals(coupon.getDiscountType())) {
            discount = amount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }

        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }

        if (discount.compareTo(amount) > 0) {
            return amount;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return discount;
    }

    private Coupon getCouponEntity(Long couponId) {
        if (couponId == null) {
            throw new RuntimeException("couponId is required");
        }
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
    }

    private SubscriptionPlan getPlanEntity(Long planId) {
        if (planId == null) {
            throw new RuntimeException("planId is required");
        }
        return subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));
    }

    private void validateRequest(CouponRequest request) {
        if (request == null) {
            throw new RuntimeException("request body is required");
        }
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new RuntimeException("code is required");
        }
        if (request.getDiscountType() == null || request.getDiscountType().trim().isEmpty()) {
            throw new RuntimeException("discountType is required");
        }
        String discountType = request.getDiscountType().trim().toUpperCase(Locale.ROOT);
        if (!TYPE_PERCENT.equals(discountType) && !TYPE_FLAT.equals(discountType)) {
            throw new RuntimeException("discountType must be PERCENT or FLAT");
        }

        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("discountValue must be greater than zero");
        }
        if (TYPE_PERCENT.equals(discountType)
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("PERCENT discountValue must be less than or equal to 100");
        }

        if (request.getUsageLimit() != null && request.getUsageLimit() <= 0) {
            throw new RuntimeException("usageLimit must be positive");
        }
        if (request.getPerUserLimit() != null && request.getPerUserLimit() <= 0) {
            throw new RuntimeException("perUserLimit must be positive");
        }

        if (request.getValidFrom() != null && request.getValidTill() != null
                && request.getValidTill().isBefore(request.getValidFrom())) {
            throw new RuntimeException("validTill must be after validFrom");
        }

        if (request.getMaxDiscountAmount() != null
                && request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("maxDiscountAmount must be greater than zero");
        }
    }

    private void applyValues(Coupon coupon, CouponRequest request) {
        coupon.setCode(request.getCode().trim().toUpperCase(Locale.ROOT));
        coupon.setDiscountType(request.getDiscountType().trim().toUpperCase(Locale.ROOT));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTill(request.getValidTill());
        coupon.setActive(request.getActive() == null ? true : request.getActive());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerUserLimit(request.getPerUserLimit());
        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }
    }

    private CouponResponse toResponse(Coupon coupon) {
        CouponResponse response = new CouponResponse();
        response.setId(coupon.getId());
        response.setCode(coupon.getCode());
        response.setDiscountType(coupon.getDiscountType());
        response.setDiscountValue(coupon.getDiscountValue());
        response.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        response.setValidFrom(coupon.getValidFrom());
        response.setValidTill(coupon.getValidTill());
        response.setActive(coupon.getActive());
        response.setUsageLimit(coupon.getUsageLimit());
        response.setPerUserLimit(coupon.getPerUserLimit());
        response.setUsedCount(coupon.getUsedCount());

        Set<Long> planIds = coupon.getPlans()
                .stream()
                .map(SubscriptionPlan::getId)
                .collect(Collectors.toSet());
        response.setPlanIds(planIds);

        return response;
    }
}

