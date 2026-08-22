package com.ecommerce.security;

import com.ecommerce.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Central place to pull the current authenticated user's id out of the
 * SecurityContext. Used by Cart, Wishlist, Order, etc. instead of each
 * controller wiring @AuthenticationPrincipal separately.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        return getCurrentUserDetails().getId();
    }

    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("No authenticated user in the current request");
        }
        return userDetails;
    }
}
