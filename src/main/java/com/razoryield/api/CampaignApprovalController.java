package com.razoryield.api;

import com.razoryield.campaign.AuditService;
import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.domain.CampaignStatus;
import com.razoryield.domain.CustomerCohort;
import com.razoryield.domain.CustomerCohortRepository;
import com.razoryield.domain.Product;
import com.razoryield.domain.ProductRepository;
import com.razoryield.gateway.RazorpayGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * The human gate. A campaign the policy engine sent here cannot become a payment link without a
 * merchant presenting a valid key and the campaign being in exactly the right state.
 */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignApprovalController {

    private static final Logger log = LoggerFactory.getLogger(CampaignApprovalController.class);

    private static final String APPROVER_USER_ID = "MERCHANT_ADMIN";
    private static final String VERDICT_MANUAL = "MANUALLY_APPROVED";
    /** Used when no cohort row is available; Razorpay requires a contact on a payment link. */
    private static final String FALLBACK_CONTACT = "+910000000000";

    private final CampaignRepository campaignRepository;
    private final AuditService auditService;
    private final RazorpayGatewayService razorpayGatewayService;
    private final ProductRepository productRepository;
    private final CustomerCohortRepository cohortRepository;
    private final String merchantApiKey;

    public CampaignApprovalController(CampaignRepository campaignRepository,
                                      AuditService auditService,
                                      RazorpayGatewayService razorpayGatewayService,
                                      ProductRepository productRepository,
                                      CustomerCohortRepository cohortRepository,
                                      @Value("${merchant.api.key:}") String merchantApiKey) {
        this.campaignRepository = campaignRepository;
        this.auditService = auditService;
        this.razorpayGatewayService = razorpayGatewayService;
        this.productRepository = productRepository;
        this.cohortRepository = cohortRepository;
        this.merchantApiKey = merchantApiKey;
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Merchant-Key", required = false) String merchantKey) {

        authorize(merchantKey);

        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.CampaignNotFoundException("No campaign with id " + id + "."));

        if (campaign.getStatus() != CampaignStatus.PENDING_MERCHANT_APPROVAL) {
            throw new ApiExceptions.InvalidCampaignStateException(
                    "Campaign " + id + " is " + campaign.getStatus()
                            + " and can only be approved from PENDING_MERCHANT_APPROVAL.");
        }

        String contact = cohortRepository.findTargetable().stream()
                .findFirst()
                .map(CustomerCohort::getPhoneNumber)
                .orElse(FALLBACK_CONTACT);

        String linkId = razorpayGatewayService.createPaymentLink(
                campaign.getId().toString(), campaign.getSku(), campaign.getOfferPricePaise(), contact);

        campaign.setStatus(CampaignStatus.APPROVED);
        campaign.setRazorpayLinkId(linkId);
        campaignRepository.save(campaign);

        Product product = productRepository.findById(campaign.getSku()).orElse(null);
        auditService.append(CampaignAuditLog.builder()
                .campaignId(campaign.getId())
                .sku(campaign.getSku())
                .costPricePaise(product == null ? 0L : product.getCostPricePaise())
                .basePricePaise(product == null ? campaign.getOfferPricePaise() : product.getBasePricePaise())
                .discountPct(campaign.getDiscountPct())
                .offerPricePaise(campaign.getOfferPricePaise())
                .llmReasoning("Merchant approved this campaign through the approval gate.")
                .gateVerdict(VERDICT_MANUAL)
                .approverUserId(APPROVER_USER_ID)
                .razorpayLinkId(linkId)
                .settlementStatus("PENDING")
                .build());

        log.info("Campaign {} approved by {} and dispatched as {}", campaign.getId(), APPROVER_USER_ID, linkId);

        return ResponseEntity.ok(Map.of(
                "campaignId", campaign.getId().toString(),
                "sku", campaign.getSku(),
                "status", campaign.getStatus().name(),
                "offerPricePaise", campaign.getOfferPricePaise(),
                "razorpayLinkId", linkId));
    }

    /** Constant-time comparison, so a wrong key cannot be discovered a character at a time. */
    private void authorize(String presentedKey) {
        if (merchantApiKey.isBlank()) {
            throw new ApiExceptions.UnauthorizedException(
                    "Approval gate is not configured; set merchant.api.key before approving campaigns.");
        }
        if (presentedKey == null || presentedKey.isBlank()) {
            throw new ApiExceptions.UnauthorizedException("Missing X-Merchant-Key header.");
        }
        boolean matches = MessageDigest.isEqual(
                presentedKey.getBytes(StandardCharsets.UTF_8),
                merchantApiKey.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new ApiExceptions.UnauthorizedException("Invalid X-Merchant-Key.");
        }
    }
}
