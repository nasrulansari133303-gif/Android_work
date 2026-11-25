package com.kaammitra.payment;

import com.kaammitra.common.PaymentMethod;
import com.kaammitra.common.PaymentStatus;

public interface PaymentGateway {
    PaymentGatewayResult initiatePayment(String jobCode, double amount, PaymentMethod method, String transactionRef);

    record PaymentGatewayResult(PaymentStatus status, String transactionRef, String message) {}
}
