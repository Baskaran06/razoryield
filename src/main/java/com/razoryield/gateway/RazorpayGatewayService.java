package com.razoryield.gateway;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates Razorpay payment links for approved campaigns. Reached only after the policy gate has
 * passed and, above the auto-dispatch thresholds, after a merchant has approved.
 */
public class RazorpayGatewayService implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGatewayService.class);

    private final String apiKey;
    private final String apiSecret;

    private RazorpayClient razorpayClient;

    public RazorpayGatewayService(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public String mode() {
        return "RAZORPAY TEST MODE (" + apiKey + ")";
    }

    void initialiseClient() {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            log.warn("Razorpay credentials are not configured. Payment link creation will fail until they are set.");
            return;
        }
        try {
            this.razorpayClient = new RazorpayClient(apiKey, apiSecret);
            log.info("Razorpay client initialised for key {}", apiKey);
        } catch (RazorpayException e) {
            throw new PaymentGatewayException("Could not initialise the Razorpay client: " + e.getMessage(), e);
        }
    }

    /** Test seam: lets a unit test supply a mocked SDK client. */
    void setRazorpayClient(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    /**
     * @param campaignId       goes out as reference_id, which is how the webhook maps a payment back
     *                         to the campaign that caused it
     * @param offerPricePaise  the amount, in paise, exactly as Razorpay expects it
     * @return the payment link id, for example {@code plink_ABC123}
     */
    @Override
    public String createPaymentLink(String campaignId, String sku, long offerPricePaise, String customerPhone) {
        if (razorpayClient == null) {
            throw new PaymentGatewayException("Razorpay client is not initialised; check razorpay.api.key and razorpay.api.secret.");
        }

        JSONObject customer = new JSONObject();
        customer.put("contact", customerPhone);

        JSONObject request = new JSONObject();
        request.put("amount", offerPricePaise);
        request.put("currency", "INR");
        request.put("accept_partial", false);
        request.put("description", "RazorYield Flash Sale for " + sku);
        request.put("reference_id", campaignId);
        request.put("customer", customer);

        try {
            PaymentLink paymentLink = razorpayClient.paymentLink.create(request);
            String linkId = paymentLink.get("id");
            log.info("Created payment link {} for campaign {} ({} paise)", linkId, campaignId, offerPricePaise);
            return linkId;
        } catch (RazorpayException e) {
            throw new PaymentGatewayException(
                    "Razorpay rejected the payment link for campaign " + campaignId + ": " + e.getMessage(), e);
        }
    }
}
