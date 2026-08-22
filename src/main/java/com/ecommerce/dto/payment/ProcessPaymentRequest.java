package com.ecommerce.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessPaymentRequest {

    @NotNull(message = "paymentMethod is required")
    private String paymentMethod;

    /**
     * Dev/test-only escape hatch for the simulated gateway (see
     * gateway/SimulatedPaymentGatewayService) so a declined charge can be
     * exercised on demand instead of relying on randomness. Has no effect on
     * COD. MUST be removed (or ignored) once a real gateway (Stripe/PayPal)
     * replaces the simulator — a real integration takes its success/failure
     * from the actual charge response, never from client input.
     */
    @Builder.Default
    private Boolean simulateFailure = false;
}
