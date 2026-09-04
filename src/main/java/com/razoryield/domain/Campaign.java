package com.razoryield.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CampaignStatus status;

    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "offer_price_paise", nullable = false)
    private long offerPricePaise;

    @Column(name = "razorpay_link_id", length = 128)
    private String razorpayLinkId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Campaign() {
    }

    public Campaign(String sku, CampaignStatus status, BigDecimal discountPct, long offerPricePaise) {
        this.sku = sku;
        this.status = status;
        this.discountPct = discountPct;
        this.offerPricePaise = offerPricePaise;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public long getOfferPricePaise() {
        return offerPricePaise;
    }

    public String getRazorpayLinkId() {
        return razorpayLinkId;
    }

    public void setRazorpayLinkId(String razorpayLinkId) {
        this.razorpayLinkId = razorpayLinkId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
