package com.ecommerce.gateway;

import com.ecommerce.entity.enums.PaymentMethod;

import java.math.BigDecimal;

/**
 * Abstraction over "actually charge the customer". PaymentServiceImpl depends
 * on this interface, not on any concrete gateway — swapping the simulated
 * implementation for a real Stripe/PayPal/Razorpay adapter later means adding
 * a new @Service implementing this interface and wiring it in (e.g. via a
 * @Profile or @ConditionalOnProperty), with zero changes to PaymentServiceImpl.
 */
public interface PaymentGatewayService {

    /**
     * Attempts to charge the given amount via the given method. Never throws
     * for a normal decline — a declined charge is a successful *call* that
     * returns success=false; callers persist that as a FAILED payment, not an
     * error response. Only throws for actual gateway/network failures, which
     * callers should treat as a 500 (the charge state is genuinely unknown).
     */
    GatewayChargeResult charge(BigDecimal amount, PaymentMethod method, boolean simulateFailure);

    /**
     * Refunds a prior successful charge. Like charge(), a declined/failed
     * refund attempt is a normal false return, not an exception — only actual
     * gateway/network failures should throw. COD has nothing to refund (no
     * money ever moved through the gateway) and should simply return true.
     */
    boolean refund(String transactionId, BigDecimal amount, PaymentMethod method);
}
