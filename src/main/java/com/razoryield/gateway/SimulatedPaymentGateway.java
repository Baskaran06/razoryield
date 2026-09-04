package com.razoryield.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Stands in when no Razorpay credentials are configured, so the approval path is demonstrable
 * without an account. It fabricates a link id and nothing else: no money moves, and the dashboard
 * says plainly that this is what is running.
 */
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    @Override
    public String mode() {
        return "SIMULATED (no Razorpay credentials configured)";
    }

    @Override
    public String createPaymentLink(String campaignId, String sku, long offerPricePaise, String customerPhone) {
        String linkId = "plink_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("Simulated payment link {} for campaign {} ({} paise, {})", linkId, campaignId, offerPricePaise, sku);
        return linkId;
    }
}
