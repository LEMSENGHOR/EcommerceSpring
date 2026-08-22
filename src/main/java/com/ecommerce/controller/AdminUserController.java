package com.ecommerce.controller;

import com.ecommerce.dto.admin.AdminUserFilterRequest;
import com.ecommerce.dto.admin.AssignRolesRequest;
import com.ecommerce.dto.admin.UpdateUserStatusRequest;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.user.UserResponse;
import com.ecommerce.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Everything under /api/admin/** is already restricted to ROLE_ADMIN by
 * SecurityConfig — no method-level @PreAuthorize needed here, but see
 * AdminUserServiceImpl for the self-protection checks (an admin can't
 * deactivate/delete/re-role themselves through these endpoints).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@Tag(name = "User (Admin)", description = "User management: search, status, roles, deletion. ADMIN only.")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Examples:
     *   GET /api/admin/users?page=0&size=20
     *   GET /api/admin/users?search=jane&status=ACTIVE
     *   GET /api/admin/users?role=ADMIN
     */
    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {

        AdminUserFilterRequest filter = AdminUserFilterRequest.builder()
                .search(search)
                .status(status)
                .role(role)
                .build();

        return ResponseEntity.ok(adminUserService.getAllUsers(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @Operation(summary = "Activate or deactivate a user",
            description = "Blocked (400) if the target is the calling admin themselves — an admin can't "
                    + "deactivate their own account through this endpoint.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, request));
    }

    @Operation(summary = "Replace a user's roles",
            description = "Blocked (400) if the target is the calling admin themselves — prevents an "
                    + "admin from accidentally stripping their own ADMIN role with no way back in.")
    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponse> assignRoles(
            @PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(adminUserService.assignRoles(id, request));
    }

    @Operation(summary = "Delete a user",
            description = "Blocked (400) if the target is the calling admin themselves, and blocked (409) "
                    + "if the user has any order history — deactivate instead to preserve that history.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
