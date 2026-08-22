package com.ecommerce.gateway;

import com.ecommerce.entity.enums.PaymentMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Placeholder gateway for local development and this project's current
 * phase — no real card/network processing happens here. Replace with a real
 * Stripe/PayPal/Razorpay adapter before going anywhere near production;
 * nothing outside this class needs to change when you do (see
 * PaymentGatewayService).
 *
 * Rules, deliberately simple:
 *   - COD never actually "charges" anything — always succeeds instantly, no
 *     transactionId (payment happens physically on delivery, out of scope
 *     for this system).
 *   - Every other method succeeds unless the caller passed simulateFailure=true
 *     (see ProcessPaymentRequest) — there is no hidden randomness, so tests
 *     and demos are deterministic.
 */
@Service
public class SimulatedPaymentGatewayService implements PaymentGatewayService {

    @Override
    public GatewayChargeResult charge(BigDecimal amount, PaymentMethod method, boolean simulateFailure) {
        if (method == PaymentMethod.COD) {
            return GatewayChargeResult.success(null);
        }

        if (simulateFailure) {
            return GatewayChargeResult.failure("Simulated decline (simulateFailure=true)");
        }

        return GatewayChargeResult.success("SIM-" + UUID.randomUUID());
    }

    @Override
    public boolean refund(String transactionId, BigDecimal amount, PaymentMethod method) {
        // Simulated gateway: refunds always succeed, no network call to fail.
        // A real adapter would call the provider's refund API here and return
        // its actual result instead of hardcoding true.
        return true;
    }
}
