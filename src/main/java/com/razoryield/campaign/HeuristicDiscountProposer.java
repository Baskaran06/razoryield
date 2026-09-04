package com.razoryield.campaign;

import com.razoryield.ai.LlmDiscountProposal;
import com.razoryield.domain.CustomerCohort;
import com.razoryield.domain.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Deterministic stand-in used when no OpenAI key is configured, or when the model call fails.
 * It produces the same record shape as the AI proposer so the policy gate downstream cannot tell
 * the difference, and the dashboard says which one produced each campaign.
 */
@Component
public class HeuristicDiscountProposer {

    public LlmDiscountProposal propose(Product product, CustomerCohort cohort) {
        int discountPct = discountFor(product.getDaysIdle());
        long offerPricePaise = (product.getBasePricePaise() * (100L - discountPct)) / 100L;

        String reasoning = ("%s has sat for %d days with %d units on hand. The targeted cohort last bought "
                + "%d days ago across %d orders. A %d%% cut takes it from %d paise to %d paise, which is the "
                + "smallest reduction this ruleset applies at that level of stagnation.")
                .formatted(product.getSku(), product.getDaysIdle(), product.getStockQty(),
                        cohort.getDaysSinceLastPurchase(), cohort.getTotalOrders(),
                        discountPct, product.getBasePricePaise(), offerPricePaise);

        return new LlmDiscountProposal(new BigDecimal(discountPct).setScale(2), offerPricePaise, reasoning);
    }

    /** Deeper cuts the longer stock has been dead, capped well below the 40% ceiling. */
    private static int discountFor(int daysIdle) {
        if (daysIdle >= 180) {
            return 30;
        }
        if (daysIdle >= 90) {
            return 20;
        }
        if (daysIdle >= 60) {
            return 15;
        }
        return 10;
    }
}
