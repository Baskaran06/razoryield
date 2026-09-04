package com.razoryield.gateway;

/**
 * Wraps anything the Razorpay SDK throws, so no SDK type escapes the gateway package.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
