package com.razoryield.gateway;

/**
 * Lets the approval gate create a payment link without knowing whether a real Razorpay account is
 * configured. Both implementations are reached only after the policy gate and the merchant.
 */
public interface PaymentGateway {

    /** Human-readable description of which implementation is live, shown on the dashboard. */
    String mode();

    String createPaymentLink(String campaignId, String sku, long offerPricePaise, String customerPhone);
}
