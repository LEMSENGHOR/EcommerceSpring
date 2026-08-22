package com.ecommerce.controller;

import com.ecommerce.dto.address.AddressRequest;
import com.ecommerce.dto.address.AddressResponse;
import com.ecommerce.dto.user.ChangePasswordRequest;
import com.ecommerce.dto.user.UpdateProfileRequest;
import com.ecommerce.dto.user.UserResponse;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every endpoint here acts on the authenticated caller only (resolved from the
 * JWT via SecurityUtils) — there is no {id} path variable anywhere in this
 * controller. Acting on another user's data is exclusively AdminUserController's
 * job, and is already restricted to ROLE_ADMIN in SecurityConfig.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
@Tag(name = "User", description = "The authenticated caller's own profile and address book.")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getMyAddresses() {
        return ResponseEntity.ok(userService.getMyAddresses());
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addAddress(request));
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long addressId, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(userService.updateAddress(addressId, request));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        userService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/addresses/{addressId}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(@PathVariable Long addressId) {
        return ResponseEntity.ok(userService.setDefaultAddress(addressId));
    }
}
