package com.razoryield.domain;

/**
 * Mirrors the chk_campaign_status CHECK constraint in V1__init_schema.sql exactly.
 */
public enum CampaignStatus {
    PROPOSED,
    PENDING_MERCHANT_APPROVAL,
    AUTO_DISPATCHED,
    APPROVED,
    REJECTED_MARGIN_BREACH,
    FAILED_RETRYABLE
}
