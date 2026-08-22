package com.ecommerce.service;

import com.ecommerce.dto.address.AddressRequest;
import com.ecommerce.dto.address.AddressResponse;
import com.ecommerce.dto.user.ChangePasswordRequest;
import com.ecommerce.dto.user.UpdateProfileRequest;
import com.ecommerce.dto.user.UserResponse;

import java.util.List;

/**
 * Self-service operations — every method acts on the currently authenticated
 * user (resolved via SecurityUtils in the impl), never on an arbitrary id.
 * Admin-on-any-user operations live in AdminUserService instead.
 */
public interface UserService {

    UserResponse getMyProfile();

    UserResponse updateMyProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    List<AddressResponse> getMyAddresses();

    AddressResponse addAddress(AddressRequest request);

    AddressResponse updateAddress(Long addressId, AddressRequest request);

    void deleteAddress(Long addressId);

    AddressResponse setDefaultAddress(Long addressId);
}
