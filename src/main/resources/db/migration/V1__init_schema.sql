-- RazorYield initial schema.
--
-- Money rule: every currency column is BIGINT holding paise. No FLOAT, DOUBLE or DECIMAL is used
-- for money anywhere in this schema. NUMERIC(5,2) appears only for discount_pct, which is a
-- percentage rate and not a currency amount.
--
-- Naming rule: a concept keeps one name everywhere. cost_price_paise, base_price_paise,
-- offer_price_paise, discount_pct, sku, razorpay_link_id and razorpay_payment_id are spelled
-- identically in every table they appear in.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE products (
    sku               VARCHAR(64) PRIMARY KEY,
    cost_price_paise  BIGINT      NOT NULL,
    base_price_paise  BIGINT      NOT NULL,
    days_idle         INT         NOT NULL DEFAULT 0,
    stock_qty         INT         NOT NULL DEFAULT 0
);

CREATE TABLE customer_cohorts (
    customer_id               VARCHAR(64) PRIMARY KEY,
    phone_number              VARCHAR(32) NOT NULL,
    days_since_last_purchase  INT         NOT NULL,
    total_orders              INT         NOT NULL
);

CREATE TABLE campaigns (
    id                 UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    sku                VARCHAR(64)   NOT NULL REFERENCES products (sku),
    status             VARCHAR(32)   NOT NULL,
    discount_pct       NUMERIC(5, 2) NOT NULL,
    offer_price_paise  BIGINT        NOT NULL,
    razorpay_link_id   VARCHAR(128),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_campaign_status CHECK (status IN (
        'PROPOSED',
        'PENDING_MERCHANT_APPROVAL',
        'AUTO_DISPATCHED',
        'APPROVED',
        'REJECTED_MARGIN_BREACH',
        'FAILED_RETRYABLE'
    ))
);

-- Append-only. Rows are inserted, never updated. A state change is a new row, not an edit.
CREATE TABLE campaign_audit_log (
    audit_id             UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    campaign_id          UUID          NOT NULL REFERENCES campaigns (id),
    sku                  VARCHAR(64)   NOT NULL,
    cost_price_paise     BIGINT        NOT NULL,
    base_price_paise     BIGINT        NOT NULL,
    discount_pct         NUMERIC(5, 2) NOT NULL,
    offer_price_paise    BIGINT        NOT NULL,
    llm_reasoning        TEXT          NOT NULL,
    gate_verdict         VARCHAR(32)   NOT NULL,
    approver_user_id     VARCHAR(64),
    razorpay_link_id     VARCHAR(128),
    razorpay_payment_id  VARCHAR(128),
    settlement_status    VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    failure_reason       VARCHAR(255),
    -- Concurrent duplicate webhook deliveries are rejected by the database itself, not by an
    -- application-level read-then-write that two threads could both pass.
    CONSTRAINT unique_payment_id UNIQUE (razorpay_payment_id)
);

CREATE INDEX idx_campaigns_status ON campaigns (status);
CREATE INDEX idx_audit_campaign_id ON campaign_audit_log (campaign_id);
CREATE INDEX idx_products_idle ON products (days_idle DESC, stock_qty DESC);
