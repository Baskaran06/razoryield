package com.razoryield.webhook;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignAuditLogRepository;
import com.razoryield.domain.CampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Settlement notifications from Razorpay.
 *
 * <p>Two invariants hold here. The audit trail stays append-only, so a settlement is a new row and
 * never an edit to an existing one. And idempotency is enforced by the database, not by a
 * read-then-write in application code: the unique constraint on razorpay_payment_id is what makes
 * two concurrent deliveries of the same event safe.
 *
 * <p>Known limitation: this relies entirely on Razorpay delivering the webhook. There is no
 * CRON-based reconciliation job to sweep up payments whose notification never arrived, which a
 * production deployment would need. Out of scope here.
 */
@RestController
@RequestMapping("/api/v1/webhooks/razorpay")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private static final String SETTLED_EVENT = "payment_link.paid";
    private static final String VERDICT_SETTLED = "WEBHOOK_SETTLED";
    private static final String SETTLEMENT_PAID = "PAID";

    private final CampaignRepository campaignRepository;
    private final CampaignAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public RazorpayWebhookController(CampaignRepository campaignRepository,
                                     CampaignAuditLogRepository auditLogRepository,
                                     ObjectMapper objectMapper,
                                     @Value("${razorpay.webhook.secret:}") String webhookSecret) {
        this.campaignRepository = campaignRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    /**
     * The body is taken as a raw String on purpose. Signature verification runs over the exact bytes
     * Razorpay signed, so letting Jackson deserialise and re-serialise first would break it.
     */
    @PostMapping
    public ResponseEntity<String> receive(@RequestBody String payload,
                                          @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        verifySignature(payload, signature);

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new WebhookProcessingException("Webhook body was not valid JSON.", e);
        }

        String event = root.path("event").asText("");
        if (!SETTLED_EVENT.equals(event)) {
            log.debug("Ignoring webhook event '{}'", event);
            return ResponseEntity.ok("ignored");
        }

        JsonNode paymentLinkEntity = root.path("payload").path("payment_link").path("entity");
        String referenceId = paymentLinkEntity.path("reference_id").asText(null);
        String razorpayLinkId = paymentLinkEntity.path("id").asText(null);
        String razorpayPaymentId = extractPaymentId(root);

        if (referenceId == null || referenceId.isBlank()) {
            throw new WebhookProcessingException("payment_link.paid carried no reference_id.");
        }
        if (razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            throw new WebhookProcessingException("payment_link.paid carried no payment id.");
        }

        UUID campaignId = parseCampaignId(referenceId);
        Optional<Campaign> campaign = campaignRepository.findById(campaignId);
        if (campaign.isEmpty()) {
            log.warn("Webhook referenced unknown campaign {}", campaignId);
            return ResponseEntity.ok("unknown campaign");
        }

        Campaign found = campaign.get();
        CampaignAuditLog settlement = CampaignAuditLog.builder()
                .campaignId(found.getId())
                .sku(found.getSku())
                .costPricePaise(0L)
                .basePricePaise(found.getOfferPricePaise())
                .discountPct(found.getDiscountPct() == null ? BigDecimal.ZERO : found.getDiscountPct())
                .offerPricePaise(found.getOfferPricePaise())
                .llmReasoning("Settlement confirmed by a signed Razorpay " + SETTLED_EVENT + " webhook.")
                .gateVerdict(VERDICT_SETTLED)
                .razorpayLinkId(razorpayLinkId != null ? razorpayLinkId : found.getRazorpayLinkId())
                .razorpayPaymentId(razorpayPaymentId)
                .settlementStatus(SETTLEMENT_PAID)
                .build();

        try {
            auditLogRepository.saveAndFlush(settlement);
            log.info("Campaign {} settled by payment {}", found.getId(), razorpayPaymentId);
        } catch (DataIntegrityViolationException duplicate) {
            // The unique constraint on razorpay_payment_id rejected a redelivery. Acknowledge it so
            // Razorpay stops retrying, without applying the side effect twice.
            log.info("Duplicate delivery for payment {} ignored by unique constraint", razorpayPaymentId);
            return ResponseEntity.ok("duplicate ignored");
        }

        return ResponseEntity.ok("ok");
    }

    private void verifySignature(String payload, String signature) {
        if (webhookSecret.isBlank()) {
            throw new WebhookSignatureMismatchException(
                    "Webhook secret is not configured; refusing to trust an unverifiable payload.");
        }
        if (signature == null || signature.isBlank()) {
            throw new WebhookSignatureMismatchException("Missing X-Razorpay-Signature header.");
        }
        boolean valid;
        try {
            valid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (RazorpayException e) {
            throw new WebhookSignatureMismatchException("Could not verify webhook signature: " + e.getMessage());
        }
        if (!valid) {
            throw new WebhookSignatureMismatchException("Webhook signature did not match.");
        }
    }

    /**
     * On a payment_link.paid event the payment sits alongside the link under payload.payment.entity.
     * Some deliveries carry it on the link entity instead, so both shapes are accepted.
     */
    private static String extractPaymentId(JsonNode root) {
        JsonNode payment = root.path("payload").path("payment").path("entity").path("id");
        if (!payment.isMissingNode() && !payment.asText("").isBlank()) {
            return payment.asText();
        }
        JsonNode onLink = root.path("payload").path("payment_link").path("entity").path("payment_id");
        return onLink.isMissingNode() ? null : onLink.asText(null);
    }

    private static UUID parseCampaignId(String referenceId) {
        try {
            return UUID.fromString(referenceId);
        } catch (IllegalArgumentException e) {
            throw new WebhookProcessingException("reference_id '" + referenceId + "' is not a campaign id.", e);
        }
    }
}
