package com.razoryield.campaign;

import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.domain.CampaignStatus;
import com.razoryield.policy.DailyBudgetDepletedException;
import com.razoryield.policy.DiscountPolicyValidator;
import com.razoryield.policy.MarginFloorBreachedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Turns a proposed discount into a persisted campaign, or into a recorded rejection.
 * <p>
 * The policy validator runs first, always. A proposal that fails it never acquires a dispatchable
 * status, and the refusal is written to the audit trail rather than being dropped.
 */
@Service
public class CampaignStateMachineService {

    private static final Logger log = LoggerFactory.getLogger(CampaignStateMachineService.class);

    /** At or below this discount rate and cash amount, a campaign dispatches without a human. */
    private static final BigDecimal AUTO_DISPATCH_MAX_PCT = new BigDecimal("10.00");
    private static final long AUTO_DISPATCH_MAX_DISCOUNT_PAISE = 50_000L;

    static final String VERDICT_AUTO_DISPATCH = "PASSED_AUTO_DISPATCH";
    static final String VERDICT_REQUIRES_APPROVAL = "REQUIRES_HUMAN_APPROVAL";
    static final String VERDICT_MARGIN_BREACH = "REJECTED_MARGIN_BREACH";
    static final String VERDICT_BUDGET_DEPLETED = "REJECTED_BUDGET_DEPLETED";

    private final CampaignRepository campaignRepository;
    private final AuditService auditService;
    private final DiscountPolicyValidator discountPolicyValidator;

    public CampaignStateMachineService(CampaignRepository campaignRepository, AuditService auditService,
                                       DiscountPolicyValidator discountPolicyValidator) {
        this.campaignRepository = campaignRepository;
        this.auditService = auditService;
        this.discountPolicyValidator = discountPolicyValidator;
    }

    /**
     * Evaluates a proposed discount and persists the resulting campaign plus its audit row.
     *
     * @throws MarginFloorBreachedException  after the rejection has been recorded
     * @throws DailyBudgetDepletedException  after the rejection has been recorded
     */
    @Transactional
    public Campaign evaluateAndPersist(String sku, long costPricePaise, long basePricePaise,
                                       BigDecimal discountPct, long offerPricePaise, String llmReasoning) {

        // Step one, without exception: the deterministic gate.
        try {
            discountPolicyValidator.validate(sku, costPricePaise, basePricePaise, offerPricePaise);
        } catch (MarginFloorBreachedException e) {
            recordRejection(sku, costPricePaise, basePricePaise, discountPct, offerPricePaise,
                    llmReasoning, VERDICT_MARGIN_BREACH, e.getMessage());
            throw e;
        } catch (DailyBudgetDepletedException e) {
            recordRejection(sku, costPricePaise, basePricePaise, discountPct, offerPricePaise,
                    llmReasoning, VERDICT_BUDGET_DEPLETED, e.getMessage());
            throw e;
        }

        long discountAmountPaise = basePricePaise - offerPricePaise;
        boolean autoDispatch = discountPct.compareTo(AUTO_DISPATCH_MAX_PCT) <= 0
                && discountAmountPaise <= AUTO_DISPATCH_MAX_DISCOUNT_PAISE;

        CampaignStatus status = autoDispatch ? CampaignStatus.AUTO_DISPATCHED : CampaignStatus.PENDING_MERCHANT_APPROVAL;
        String verdict = autoDispatch ? VERDICT_AUTO_DISPATCH : VERDICT_REQUIRES_APPROVAL;

        // Saved first so the audit row can reference a real campaign id.
        Campaign campaign = campaignRepository.save(new Campaign(sku, status, discountPct, offerPricePaise));

        auditService.append(CampaignAuditLog.builder()
                .campaignId(campaign.getId())
                .sku(sku)
                .costPricePaise(costPricePaise)
                .basePricePaise(basePricePaise)
                .discountPct(discountPct)
                .offerPricePaise(offerPricePaise)
                .llmReasoning(llmReasoning)
                .gateVerdict(verdict)
                .settlementStatus("PENDING")
                .build());

        log.info("Campaign {} for {} resolved to {} ({} paise off base)",
                campaign.getId(), sku, status, discountAmountPaise);
        return campaign;
    }

    /**
     * A refused proposal is still a campaign, so that the refusal has something to hang off and the
     * audit trail stays complete.
     */
    private void recordRejection(String sku, long costPricePaise, long basePricePaise, BigDecimal discountPct,
                                 long offerPricePaise, String llmReasoning, String verdict, String failureReason) {
        Campaign rejected = campaignRepository.save(
                new Campaign(sku, CampaignStatus.REJECTED_MARGIN_BREACH, discountPct, offerPricePaise));

        auditService.append(CampaignAuditLog.builder()
                .campaignId(rejected.getId())
                .sku(sku)
                .costPricePaise(costPricePaise)
                .basePricePaise(basePricePaise)
                .discountPct(discountPct)
                .offerPricePaise(offerPricePaise)
                .llmReasoning(llmReasoning)
                .gateVerdict(verdict)
                .settlementStatus("PENDING")
                .failureReason(truncate(failureReason))
                .build());

        log.warn("Campaign for {} rejected: {} - {}", sku, verdict, failureReason);
    }

    /** failure_reason is VARCHAR(255) in the schema. */
    private static String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= 255 ? reason : reason.substring(0, 255);
    }
}
