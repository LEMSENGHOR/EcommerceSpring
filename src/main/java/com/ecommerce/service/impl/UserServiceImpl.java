package com.ecommerce.service.impl;

import com.ecommerce.dto.address.AddressRequest;
import com.ecommerce.dto.address.AddressResponse;
import com.ecommerce.dto.user.ChangePasswordRequest;
import com.ecommerce.dto.user.UpdateProfileRequest;
import com.ecommerce.dto.user.UserResponse;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.User;
import com.ecommerce.exception.InvalidCredentialsException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.AddressMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------
    // Profile
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return UserMapper.toResponse(findCurrentUserOrThrow());
    }

    @Override
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        User user = findCurrentUserOrThrow();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        return UserMapper.toResponse(user);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = findCurrentUserOrThrow();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Deliberately does NOT revoke existing refresh tokens / force re-login here.
        // Phase 15 or a security hardening pass may want to add
        // refreshTokenService.revokeAllForUser(user.getId()) so other sessions
        // are kicked out on password change — flagging rather than doing it silently.
    }

    // ---------------------------------------------------------
    // Addresses
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        return AddressMapper.toResponseList(addressRepository.findByUserId(SecurityUtils.getCurrentUserId()));
    }

    @Override
    public AddressResponse addAddress(AddressRequest request) {
        User user = findCurrentUserOrThrow();

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearExistingDefaultAddress(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        return AddressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    public AddressResponse updateAddress(Long addressId, AddressRequest request) {
        Address address = findOwnedAddressOrThrow(addressId);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearExistingDefaultAddress(address.getUser().getId());
        }

        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));

        return AddressMapper.toResponse(address);
    }

    @Override
    public void deleteAddress(Long addressId) {
        Address address = findOwnedAddressOrThrow(addressId);
        // Orders (Phase 10) reference addresses via shipping_address_id with no
        // cascade/set-null, so deleting an address already used on an order will
        // surface as a raw DB constraint error (500) — same unresolved pattern
        // flagged for Category/Brand/Product deletion, to be handled in Phase 15.
        addressRepository.delete(address);
    }

    @Override
    public AddressResponse setDefaultAddress(Long addressId) {
        Address address = findOwnedAddressOrThrow(addressId);
        clearExistingDefaultAddress(address.getUser().getId());
        address.setIsDefault(true);
        return AddressMapper.toResponse(address);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private User findCurrentUserOrThrow() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Address findOwnedAddressOrThrow(Long addressId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId));
    }

    private void clearExistingDefaultAddress(Long userId) {
        addressRepository.findByUserId(userId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .forEach(a -> a.setIsDefault(false));
    }
}
