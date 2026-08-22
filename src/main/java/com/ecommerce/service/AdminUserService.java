package com.ecommerce.service;

import com.ecommerce.dto.admin.AdminUserFilterRequest;
import com.ecommerce.dto.admin.AssignRolesRequest;
import com.ecommerce.dto.admin.UpdateUserStatusRequest;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.user.UserResponse;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    PagedResponse<UserResponse> getAllUsers(AdminUserFilterRequest filter, Pageable pageable);

    UserResponse getUserById(Long id);

    UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request);

    UserResponse assignRoles(Long id, AssignRolesRequest request);

    void deleteUser(Long id);
}
