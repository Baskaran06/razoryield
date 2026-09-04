-- RazorYield seed data. All money values are paise.

INSERT INTO products (sku, cost_price_paise, base_price_paise, days_idle, stock_qty) VALUES
    ('SKU-TEA-250G',      12000,  24000,  12,  240),
    ('SKU-COFFEE-500G',   25000,  42000,  68,   95),
    ('SKU-SOAP-4PK',       8800,  18000,   4,  610),
    ('SKU-RICE-10KG',     52000,  78000,  21,  130),
    ('SKU-BULB-9W',        6200,  13000,  91,  420),
    -- Phase 1 happy-path fixture: floor is 45000 * 115 / 100 = 51750, so an offer of 90000 passes.
    ('SKU-HEADPHONE-BT',  45000, 120000,  57,   78),
    ('SKU-KETTLE-1L',     90000, 160000, 103,   34),
    ('SKU-BACKPACK-30L',  70000, 140000,  45,  112),
    ('SKU-YOGAMAT-6MM',   38000,  70000,  76,  188),
    -- MARGIN-BREACH TEST FIXTURE.
    -- The 15% floor is 95000 * 115 / 100 = 109250 paise, which is already above the 100000 paise
    -- base price. Every possible discount on this SKU therefore breaches the margin floor, which
    -- is exactly what Phase 1's DiscountPolicyValidator test asserts against.
    ('SKU-LEGACY-PRINTER', 95000, 100000, 214,   19);

-- Every cohort row satisfies the targeting criteria: days_since_last_purchase >= 45 AND total_orders >= 2.
INSERT INTO customer_cohorts (customer_id, phone_number, days_since_last_purchase, total_orders) VALUES
    ('CUST-1001', '+919840421877',  62,  14),
    ('CUST-1002', '+919003155420',  45,   2),
    ('CUST-1003', '+919962570113', 118,   9),
    ('CUST-1004', '+919884760294',  87,  23),
    ('CUST-1005', '+918939614508',  51,   4);
