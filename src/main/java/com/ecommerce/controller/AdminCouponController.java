package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.coupon.AdminCouponFilterRequest;
import com.ecommerce.dto.coupon.BulkGenerateCouponRequest;
import com.ecommerce.dto.coupon.CouponRequest;
import com.ecommerce.dto.coupon.CouponResponse;
import com.ecommerce.dto.coupon.CouponUsageResponse;
import com.ecommerce.service.AdminCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Everything here is already restricted to ROLE_ADMIN by SecurityConfig. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/coupons")
@Tag(name = "Coupon (Admin)", description = "Coupon CRUD, bulk generation, and usage reporting. ADMIN only.")
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCouponService.createCoupon(request));
    }

    @Operation(summary = "Generate a batch of unique coupon codes",
            description = "All generated codes share the same discount rules from the request; each gets "
                    + "its own randomly-generated code (8 chars, ambiguous characters like 0/O and 1/I "
                    + "excluded), checked for uniqueness within the batch in addition to the database's "
                    + "own UNIQUE(code) constraint.")
    @PostMapping("/bulk-generate")
    public ResponseEntity<List<CouponResponse>> bulkGenerateCoupons(
            @Valid @RequestBody BulkGenerateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCouponService.bulkGenerateCoupons(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CouponResponse>> getAllCoupons(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @PageableDefault(size = 20) Pageable pageable) {

        AdminCouponFilterRequest filter = AdminCouponFilterRequest.builder()
                .status(status)
                .code(code)
                .build();

        return ResponseEntity.ok(adminCouponService.getAllCoupons(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> getCouponById(@PathVariable Long id) {
        return ResponseEntity.ok(adminCouponService.getCouponById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(adminCouponService.updateCoupon(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CouponResponse> deactivateCoupon(@PathVariable Long id) {
        return ResponseEntity.ok(adminCouponService.deactivateCoupon(id));
    }

    @Operation(summary = "Permanently delete a coupon",
            description = "Blocked with 409 if the coupon has ever been redeemed (usage history is "
                    + "preserved) — use deactivate instead to stop a coupon from being used further "
                    + "while keeping its records.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        adminCouponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<List<CouponUsageResponse>> getCouponUsages(@PathVariable Long id) {
        return ResponseEntity.ok(adminCouponService.getCouponUsages(id));
    }
}
