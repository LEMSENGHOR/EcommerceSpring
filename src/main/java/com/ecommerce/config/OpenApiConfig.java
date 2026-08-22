package com.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Everything here is *global* API documentation — metadata, servers, and the
 * bearer-JWT security scheme that makes Swagger UI's "Authorize" button work.
 * Per-endpoint documentation (@Tag on each controller, @Operation on endpoints
 * whose behavior isn't obvious from the URL alone) lives on the controllers
 * themselves, not here — see README for which endpoints got @Operation and why
 * most didn't need it.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce API")
                        .description("""
                                REST API for the e-commerce platform built across Phases 1–15: \
                                catalog (Category/Brand/Product), auth (JWT access + refresh tokens), \
                                cart/wishlist, checkout (Order/Payment/Coupon), and post-purchase \
                                (Review/Notification). Admin-only endpoints are grouped under \
                                /api/admin/** and require a user with the ADMIN role.

                                To call an authenticated endpoint from this UI: POST /api/auth/login, \
                                copy the accessToken from the response, then click **Authorize** above \
                                and paste it in (no need to type "Bearer " — that's added automatically).""")
                        .version("v1")
                        .contact(new Contact().name("Engineering")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the accessToken from POST /api/auth/login or /api/auth/refresh.")));
    }
}
