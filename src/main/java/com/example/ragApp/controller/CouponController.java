package com.example.ragApp.controller;

import com.example.ragApp.dto.ApiMessageResponse;
import com.example.ragApp.dto.CouponRequest;
import com.example.ragApp.dto.CouponResponse;
import com.example.ragApp.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/admin")
    public ResponseEntity<CouponResponse> createCoupon(@RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<CouponResponse>> getCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/admin/{couponId}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.getCouponById(couponId));
    }

    @PutMapping("/admin/{couponId}")
    public ResponseEntity<CouponResponse> updateCoupon(@PathVariable Long couponId,
                                                       @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(couponId, request));
    }

    @DeleteMapping("/admin/{couponId}")
    public ResponseEntity<ApiMessageResponse> deleteCoupon(@PathVariable Long couponId) {
        couponService.deactivateCoupon(couponId);
        return ResponseEntity.ok(new ApiMessageResponse("Coupon deactivated"));
    }

    @PostMapping("/admin/{couponId}/plans/{planId}")
    public ResponseEntity<CouponResponse> attachCouponToPlan(@PathVariable Long couponId,
                                                             @PathVariable Long planId) {
        return ResponseEntity.ok(couponService.attachPlan(couponId, planId));
    }

    @DeleteMapping("/admin/{couponId}/plans/{planId}")
    public ResponseEntity<CouponResponse> detachCouponFromPlan(@PathVariable Long couponId,
                                                               @PathVariable Long planId) {
        return ResponseEntity.ok(couponService.detachPlan(couponId, planId));
    }
}

