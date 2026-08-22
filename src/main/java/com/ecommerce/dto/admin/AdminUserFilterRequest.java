package com.ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bound from query params on GET /api/admin/users. All fields optional. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserFilterRequest {
    /** Matches against name OR email, case-insensitive, partial match. */
    private String search;
    private String status;
    /** Role name, e.g. "ADMIN" or "USER". */
    private String role;
}
