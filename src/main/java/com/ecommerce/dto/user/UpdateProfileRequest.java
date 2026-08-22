package com.ecommerce.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deliberately excludes email. Changing a login email is a security-sensitive
 * operation (should involve re-verification) and is out of scope for this phase —
 * see the README note. Deliberately excludes password too; that has its own
 * endpoint (ChangePasswordRequest) requiring the current password.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;
}
