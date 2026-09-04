package com.razoryield.campaign;

import java.util.List;

/**
 * What one orchestration cycle did, including the proposals that were refused. A refusal is a
 * result, not an error to be swallowed.
 */
public record OrchestrationResult(
        int productsScanned,
        int proposalsRaised,
        String proposerUsed,
        List<Outcome> outcomes
) {
    public record Outcome(
            String sku,
            String campaignId,
            String status,
            String gateVerdict,
            long offerPricePaise,
            String detail
    ) {
    }
}
