package com.ecommerce.service.impl;

import com.ecommerce.dto.admin.AdminUserFilterRequest;
import com.ecommerce.dto.admin.AssignRolesRequest;
import com.ecommerce.dto.admin.UpdateUserStatusRequest;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.user.UserResponse;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.enums.EntityStatus;
import com.ecommerce.entity.enums.RoleName;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceInUseException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.specification.UserSpecification;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(AdminUserFilterRequest filter, Pageable pageable) {
        Page<User> page = userRepository.findAll(UserSpecification.withFilters(filter), pageable);
        return PagedResponse.from(page.map(UserMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserMapper.toResponse(findUserOrThrow(id));
    }

    @Override
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        assertNotActingOnSelf(id, "deactivate or reactivate your own account");

        User user = findUserOrThrow(id);
        user.setStatus(EntityStatus.valueOf(request.getStatus().toUpperCase()));
        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse assignRoles(Long id, AssignRolesRequest request) {
        assertNotActingOnSelf(id, "change your own roles");

        User user = findUserOrThrow(id);

        Set<Role> resolvedRoles = new LinkedHashSet<>();
        for (String roleName : request.getRoles()) {
            RoleName parsed = parseRoleNameOrThrow(roleName);
            Role role = roleRepository.findByName(parsed)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            resolvedRoles.add(role);
        }

        user.setRoles(resolvedRoles);
        return UserMapper.toResponse(user);
    }

    @Override
    public void deleteUser(Long id) {
        assertNotActingOnSelf(id, "delete your own account");

        User user = findUserOrThrow(id);

        // Order history must outlive a deleted account — pre-check and fail
        // cleanly (409) rather than a raw FK violation on fk_order_user
        // (which has no cascade, deliberately). Resolves the gap flagged
        // since Phase 7. cart/wishlist/reviews/notifications/addresses/
        // refresh_tokens/coupon_usages/user_roles all cascade-delete fine.
        if (orderRepository.existsByUserId(id)) {
            throw new ResourceInUseException(
                    "User '" + user.getEmail() + "' has order history and cannot be deleted. "
                            + "Deactivate the account instead (see updateUserStatus) to preserve that history.");
        }

        userRepository.delete(user);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private RoleName parseRoleNameOrThrow(String roleName) {
        try {
            return RoleName.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown role: " + roleName);
        }
    }

    /**
     * Prevents an admin from locking themselves out (deactivating their own
     * account, stripping their own ADMIN role, or deleting themselves) via
     * these admin-only endpoints. They can still update their own profile
     * through the self-service UserService.
     */
    private void assertNotActingOnSelf(Long targetUserId, String action) {
        if (targetUserId.equals(SecurityUtils.getCurrentUserId())) {
            throw new InvalidRequestException("You cannot " + action + " through this endpoint");
        }
    }
}
