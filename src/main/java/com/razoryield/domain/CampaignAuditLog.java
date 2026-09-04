package com.razoryield.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only. There are deliberately no setters for the money or verdict fields: a state change
 * is recorded as a new row, never as an edit to an existing one.
 */
@Entity
@Table(name = "campaign_audit_log")
public class CampaignAuditLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "audit_id", updatable = false, nullable = false)
    private UUID auditId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "cost_price_paise", nullable = false)
    private long costPricePaise;

    @Column(name = "base_price_paise", nullable = false)
    private long basePricePaise;

    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "offer_price_paise", nullable = false)
    private long offerPricePaise;

    @Column(name = "llm_reasoning", nullable = false, columnDefinition = "TEXT")
    private String llmReasoning;

    @Column(name = "gate_verdict", nullable = false, length = 32)
    private String gateVerdict;

    @Column(name = "approver_user_id", length = 64)
    private String approverUserId;

    @Column(name = "razorpay_link_id", length = 128)
    private String razorpayLinkId;

    @Column(name = "razorpay_payment_id", length = 128, unique = true)
    private String razorpayPaymentId;

    @Column(name = "settlement_status", nullable = false, length = 32)
    private String settlementStatus = "PENDING";

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    protected CampaignAuditLog() {
    }

    private CampaignAuditLog(Builder builder) {
        this.campaignId = builder.campaignId;
        this.sku = builder.sku;
        this.costPricePaise = builder.costPricePaise;
        this.basePricePaise = builder.basePricePaise;
        this.discountPct = builder.discountPct;
        this.offerPricePaise = builder.offerPricePaise;
        this.llmReasoning = builder.llmReasoning;
        this.gateVerdict = builder.gateVerdict;
        this.approverUserId = builder.approverUserId;
        this.razorpayLinkId = builder.razorpayLinkId;
        this.razorpayPaymentId = builder.razorpayPaymentId;
        this.settlementStatus = builder.settlementStatus;
        this.failureReason = builder.failureReason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getAuditId() {
        return auditId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public String getSku() {
        return sku;
    }

    public long getCostPricePaise() {
        return costPricePaise;
    }

    public long getBasePricePaise() {
        return basePricePaise;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public long getOfferPricePaise() {
        return offerPricePaise;
    }

    public String getLlmReasoning() {
        return llmReasoning;
    }

    public String getGateVerdict() {
        return gateVerdict;
    }

    public String getApproverUserId() {
        return approverUserId;
    }

    public String getRazorpayLinkId() {
        return razorpayLinkId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getDisplayName() {
        if (sku == null) return "Unknown Product";
        return switch (sku) {
            case "SKU-TEA-250G" -> "Organic Green Tea — 250g";
            case "SKU-COFFEE-500G" -> "Premium Coffee — 500g";
            case "SKU-SOAP-4PK" -> "Gentle Care Body Soap — 4 Pack";
            case "SKU-RICE-10KG" -> "Basmati Rice — 10kg";
            case "SKU-BULB-9W" -> "9W LED Smart Bulb";
            case "SKU-HEADPHONE-BT" -> "Wireless Bluetooth Headphones";
            case "SKU-KETTLE-1L" -> "1L Electric Water Kettle";
            case "SKU-BACKPACK-30L" -> "30L Waterproof Travel Backpack";
            case "SKU-YOGAMAT-6MM" -> "6mm Anti-Slip Yoga Mat";
            case "SKU-LEGACY-PRINTER" -> "Compact Desktop Inkjet Printer";
            default -> sku.replace("SKU-", "").replace("-", " ");
        };
    }

    public String getHumanVerdict() {
        if (gateVerdict == null) return "Recorded Event";
        return switch (gateVerdict) {
            case "PROPOSED" -> "Campaign Recommendation Created";
            case "PENDING_MERCHANT_APPROVAL" -> "Merchant Approval Required";
            case "AUTO_DISPATCHED" -> "Campaign Auto-Dispatched";
            case "APPROVED" -> "Merchant Approved Campaign";
            case "REJECTED_MARGIN_BREACH" -> "Campaign Rejected by Margin Guardrail";
            case "REJECTED_BUDGET_DEPLETED" -> "Campaign Rejected by Budget Cap";
            case "WEBHOOK_SETTLED" -> "Payment Received via Razorpay";
            case "FAILED_RETRYABLE" -> "Gateway Exception (Retryable)";
            default -> gateVerdict.replace("_", " ");
        };
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "Just now";
        java.time.ZonedDateTime zdt = createdAt.atZone(java.time.ZoneId.of("Asia/Kolkata"));
        return java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss").format(zdt);
    }

    public String getFormattedShortCreatedAt() {
        if (createdAt == null) return "Just now";
        java.time.ZonedDateTime zdt = createdAt.atZone(java.time.ZoneId.of("Asia/Kolkata"));
        return java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm").format(zdt);
    }

    public String getFormattedDate() {
        if (createdAt == null) return "Just now";
        java.time.ZonedDateTime zdt = createdAt.atZone(java.time.ZoneId.of("Asia/Kolkata"));
        return java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").format(zdt);
    }

    public static final class Builder {
        private UUID campaignId;
        private String sku;
        private long costPricePaise;
        private long basePricePaise;
        private BigDecimal discountPct;
        private long offerPricePaise;
        private String llmReasoning;
        private String gateVerdict;
        private String approverUserId;
        private String razorpayLinkId;
        private String razorpayPaymentId;
        private String settlementStatus = "PENDING";
        private String failureReason;

        public Builder campaignId(UUID campaignId) {
            this.campaignId = campaignId;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder costPricePaise(long costPricePaise) {
            this.costPricePaise = costPricePaise;
            return this;
        }

        public Builder basePricePaise(long basePricePaise) {
            this.basePricePaise = basePricePaise;
            return this;
        }

        public Builder discountPct(BigDecimal discountPct) {
            this.discountPct = discountPct;
            return this;
        }

        public Builder offerPricePaise(long offerPricePaise) {
            this.offerPricePaise = offerPricePaise;
            return this;
        }

        public Builder llmReasoning(String llmReasoning) {
            this.llmReasoning = llmReasoning;
            return this;
        }

        public Builder gateVerdict(String gateVerdict) {
            this.gateVerdict = gateVerdict;
            return this;
        }

        public Builder approverUserId(String approverUserId) {
            this.approverUserId = approverUserId;
            return this;
        }

        public Builder razorpayLinkId(String razorpayLinkId) {
            this.razorpayLinkId = razorpayLinkId;
            return this;
        }

        public Builder razorpayPaymentId(String razorpayPaymentId) {
            this.razorpayPaymentId = razorpayPaymentId;
            return this;
        }

        public Builder settlementStatus(String settlementStatus) {
            this.settlementStatus = settlementStatus;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public CampaignAuditLog build() {
            return new CampaignAuditLog(this);
        }
    }
}
