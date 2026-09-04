package com.razoryield.ai;

import java.math.BigDecimal;

/**
 * What the model is allowed to return. It is a proposal and nothing more: the discount only becomes
 * real after {@code DiscountPolicyValidator} and, above the auto-dispatch thresholds, a merchant.
 */
public record LlmDiscountProposal(
        BigDecimal discountPct,
        long offerPricePaise,
        String llmReasoning
) {
}
