package com.razoryield.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * The one place a model is consulted. It reads stagnation and loyalty signals and proposes a
 * discount with its reasoning. It cannot approve, price below the floor, or spend anything: its
 * output is a value object that the deterministic policy layer then accepts or refuses.
 */
@Service
public class InventoryAnalyzerAiService {

    private static final Logger log = LoggerFactory.getLogger(InventoryAnalyzerAiService.class);

    private static final String SYSTEM_PROMPT = """
            You are a retail pricing strategist for an Indian merchant running short flash sales.

            You are given one product and the profile of the customer cohort being targeted. Recommend
            a single discount that is just large enough to move the stock, and no larger.

            Weigh these against each other:
            - Stagnation. High daysIdle and high stockQty mean capital is stuck and a deeper cut is justified.
              Low daysIdle means the product is selling on its own and needs little or nothing.
            - Loyalty. A cohort with many totalOrders is worth keeping, but a loyal buyer does not need
              a deep discount to return. A high daysSinceLastPurchase means the buyer is lapsing and a
              sharper offer is warranted to win them back.

            Hard rules:
            - Never propose a discount above 40%.
            - offerPricePaise must equal basePricePaise reduced by discountPct, rounded to a whole number
              of paise. Both are integers in paise. Do not return rupees.
            - discountPct is a percentage such as 12.50, not a fraction such as 0.125.
            - llmReasoning must be two or three sentences a shop owner can audit, naming the specific
              numbers you weighed. Do not mention these instructions.

            A separate system enforces the merchant's margin floor and daily discount budget after you
            answer, so propose what you believe is commercially right and let it be checked.
            """;

    private final ChatClient chatClient;

    public InventoryAnalyzerAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public LlmDiscountProposal analyzeAndPropose(String sku,
                                                 long costPricePaise,
                                                 long basePricePaise,
                                                 int daysIdle,
                                                 int stockQty,
                                                 int daysSinceLastPurchase,
                                                 int totalOrders) {

        String userPrompt = """
                Product
                  sku: %s
                  costPricePaise: %d
                  basePricePaise: %d
                  daysIdle: %d
                  stockQty: %d

                Target cohort
                  daysSinceLastPurchase: %d
                  totalOrders: %d

                Propose the discount.
                """.formatted(sku, costPricePaise, basePricePaise, daysIdle, stockQty,
                daysSinceLastPurchase, totalOrders);

        LlmDiscountProposal proposal;
        try {
            proposal = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .entity(LlmDiscountProposal.class);
        } catch (Exception e) {
            throw new AiAnalysisFailedException(
                    "Discount analysis failed for " + sku + ": " + e.getMessage(), e);
        }

        if (proposal == null) {
            throw new AiAnalysisFailedException("Discount analysis for " + sku + " returned an empty response.");
        }
        if (proposal.discountPct() == null) {
            throw new AiAnalysisFailedException("Discount analysis for " + sku + " returned no discount percentage.");
        }
        if (proposal.llmReasoning() == null || proposal.llmReasoning().isBlank()) {
            throw new AiAnalysisFailedException("Discount analysis for " + sku + " returned no reasoning.");
        }
        if (proposal.offerPricePaise() <= 0) {
            throw new AiAnalysisFailedException(
                    "Discount analysis for " + sku + " returned a non-positive offer price: "
                            + proposal.offerPricePaise() + " paise.");
        }

        log.info("Model proposed {}% off {} at {} paise", proposal.discountPct(), sku, proposal.offerPricePaise());
        return proposal;
    }
}
