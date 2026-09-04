package com.razoryield.campaign;

import com.razoryield.ai.AiAnalysisFailedException;
import com.razoryield.ai.InventoryAnalyzerAiService;
import com.razoryield.ai.LlmDiscountProposal;
import com.razoryield.domain.Campaign;
import com.razoryield.domain.CustomerCohort;
import com.razoryield.domain.CustomerCohortRepository;
import com.razoryield.domain.Product;
import com.razoryield.domain.ProductRepository;
import com.razoryield.policy.DailyBudgetDepletedException;
import com.razoryield.policy.MarginFloorBreachedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The trigger the rest of the system hangs off. It finds stagnant stock, gets a discount proposed
 * for it, and hands each proposal to the state machine, which is where the policy gate decides
 * whether it survives.
 *
 * <p>Nothing here decides anything about money. It sequences: scan, propose, submit.
 */
@Service
public class CampaignOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(CampaignOrchestrationService.class);

    /** A product is a candidate once it has been idle this long. */
    private static final int MIN_DAYS_IDLE = 45;

    private final ProductRepository productRepository;
    private final CustomerCohortRepository cohortRepository;
    private final InventoryAnalyzerAiService aiProposer;
    private final HeuristicDiscountProposer heuristicProposer;
    private final CampaignStateMachineService stateMachine;
    private final boolean aiEnabled;

    public CampaignOrchestrationService(ProductRepository productRepository,
                                        CustomerCohortRepository cohortRepository,
                                        InventoryAnalyzerAiService aiProposer,
                                        HeuristicDiscountProposer heuristicProposer,
                                        CampaignStateMachineService stateMachine,
                                        @Value("${spring.ai.openai.api-key:not-configured}") String openAiKey) {
        this.productRepository = productRepository;
        this.cohortRepository = cohortRepository;
        this.aiProposer = aiProposer;
        this.heuristicProposer = heuristicProposer;
        this.stateMachine = stateMachine;
        this.aiEnabled = !"not-configured".equals(openAiKey) && !openAiKey.isBlank();
    }

    public String proposerLabel() {
        return aiEnabled ? "LLM via Spring AI" : "HEURISTIC (no OpenAI key configured)";
    }

    public OrchestrationResult runCycle() {
        List<Product> stagnant = productRepository.findStagnant(MIN_DAYS_IDLE);
        List<CustomerCohort> cohorts = cohortRepository.findTargetable();

        if (cohorts.isEmpty()) {
            log.warn("No cohort meets the targeting criteria; nothing to run a flash sale at.");
            return new OrchestrationResult(stagnant.size(), 0, proposerLabel(), List.of());
        }
        CustomerCohort cohort = cohorts.getFirst();

        List<OrchestrationResult.Outcome> outcomes = new ArrayList<>();
        for (Product product : stagnant) {
            outcomes.add(evaluate(product, cohort));
        }

        log.info("Orchestration cycle scanned {} stagnant products and raised {} proposals",
                stagnant.size(), outcomes.size());
        return new OrchestrationResult(stagnant.size(), outcomes.size(), proposerLabel(), outcomes);
    }

    private OrchestrationResult.Outcome evaluate(Product product, CustomerCohort cohort) {
        LlmDiscountProposal proposal = propose(product, cohort);

        try {
            Campaign campaign = stateMachine.evaluateAndPersist(
                    product.getSku(),
                    product.getCostPricePaise(),
                    product.getBasePricePaise(),
                    proposal.discountPct(),
                    proposal.offerPricePaise(),
                    proposal.llmReasoning());

            return new OrchestrationResult.Outcome(
                    product.getSku(),
                    campaign.getId().toString(),
                    campaign.getStatus().name(),
                    campaign.getStatus().name(),
                    campaign.getOfferPricePaise(),
                    proposal.llmReasoning());

        } catch (MarginFloorBreachedException e) {
            return new OrchestrationResult.Outcome(product.getSku(), null, "REJECTED_MARGIN_BREACH",
                    "REJECTED_MARGIN_BREACH", proposal.offerPricePaise(), e.getMessage());
        } catch (DailyBudgetDepletedException e) {
            return new OrchestrationResult.Outcome(product.getSku(), null, "REJECTED_MARGIN_BREACH",
                    "REJECTED_BUDGET_DEPLETED", proposal.offerPricePaise(), e.getMessage());
        }
    }

    /**
     * Falls back to the heuristic rather than skipping the product, so a model outage degrades the
     * quality of the proposal instead of stalling the merchant's inventory.
     */
    private LlmDiscountProposal propose(Product product, CustomerCohort cohort) {
        if (!aiEnabled) {
            return heuristicProposer.propose(product, cohort);
        }
        try {
            return aiProposer.analyzeAndPropose(
                    product.getSku(),
                    product.getCostPricePaise(),
                    product.getBasePricePaise(),
                    product.getDaysIdle(),
                    product.getStockQty(),
                    cohort.getDaysSinceLastPurchase(),
                    cohort.getTotalOrders());
        } catch (AiAnalysisFailedException e) {
            log.warn("Model failed for {}, falling back to the heuristic: {}", product.getSku(), e.getMessage());
            return heuristicProposer.propose(product, cohort);
        }
    }
}
