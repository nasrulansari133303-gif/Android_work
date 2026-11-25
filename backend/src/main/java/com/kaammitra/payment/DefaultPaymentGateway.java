package com.kaammitra.payment;

import com.kaammitra.common.PaymentMethod;
import com.kaammitra.common.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayResult initiatePayment(String jobCode, double amount, PaymentMethod method, String transactionRef) {
        if (method == PaymentMethod.CASH) {
            String ref = "CASH-" + jobCode;
            return new PaymentGatewayResult(PaymentStatus.PAID, ref, "Cash settlement recorded");
        }
        if (method == PaymentMethod.UPI) {
            if (transactionRef != null && !transactionRef.isBlank()) {
                return new PaymentGatewayResult(PaymentStatus.PAID, transactionRef.trim(), "UPI reference verified");
            }
            String pendingRef = "UPI-PEND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new PaymentGatewayResult(PaymentStatus.PENDING, pendingRef,
                    "Awaiting UPI payment confirmation");
        }
        // ONLINE payments must NOT be falsely marked PAID without gateway webhook/confirmation
        if (transactionRef != null && transactionRef.startsWith("GW_CONFIRMED_")) {
            return new PaymentGatewayResult(PaymentStatus.PAID, transactionRef, "Online gateway payment confirmed");
        }
        String gatewayOrderRef = "ORD-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        return new PaymentGatewayResult(PaymentStatus.PENDING, gatewayOrderRef,
                "Payment initiated. Awaiting gateway confirmation.");
    }
}
