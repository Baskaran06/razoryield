# RazorYield

An autonomous margin-guarded flash sale orchestrator. It finds stagnant stock, asks a model what
discount would move it, and then refuses to act on that answer until the discount has cleared a
margin floor, a daily budget, and — above a threshold — a human being.

## The rule the whole system is built around

A model proposes. Deterministic code disposes.

| Stage | Component | Model involved? |
|-------|-----------|-----------------|
| Propose a discount and explain it | `InventoryAnalyzerAiService` | **Yes** |
| Enforce the margin floor | `DiscountPolicyValidator` | No |
| Reserve against the daily budget | `DiscountPolicyValidator` | No |
| Decide auto-dispatch vs human approval | `CampaignStateMachineService` | No |
| Human approval | `CampaignApprovalController` | No |
| Create the payment link | `RazorpayGatewayService` | No |
| Confirm settlement | `RazorpayWebhookController` | No |
| Record every step | `AuditService` | No |

The model returns an `LlmDiscountProposal` — a value object, never an action. Everything downstream
treats it as untrusted input.

## Money

Every currency value is a `long` of paise, in Java and in PostgreSQL (`BIGINT`). No `float`,
`double`, or `BigDecimal` touches a money calculation anywhere. The margin floor is computed with
integer arithmetic that multiplies before dividing, so nothing is ever promoted to floating point:

```java
long floorPricePaise = (costPricePaise * (100L + globalMinMarginPct)) / 100L;
```

`BigDecimal` appears only for `discount_pct`, which is a percentage rate, not an amount of money.

## The three gates

**Margin floor.** Default 15%, configurable via `orchestrator.policy.global-min-margin-pct`. An
offer below `cost × 1.15` is refused before Redis is even contacted.

**Daily budget.** ₹20,000 (2,000,000 paise) per day, held in Redis under
`merchant:default:budget:yyyy-MM-dd` with a 24-hour TTL. The reservation is a single atomic
`INCRBY`, checked afterwards and compensated with a negative `INCRBY` if it overshot. Reserving
first and compensating second is what makes it safe under concurrency — two callers cannot both
read a stale total and both conclude there is room.

**Human approval.** At or below 10.00% *and* at or below ₹500 of cash discount, a campaign
auto-dispatches. Past either threshold it waits at `PENDING_MERCHANT_APPROVAL` for a merchant
holding a valid `X-Merchant-Key`.

## The audit trail

`campaign_audit_log` is append-only. A state change is a new row, never an edit — the entity
deliberately exposes no setters. Settlement is recorded by inserting a `WEBHOOK_SETTLED` row, not by
updating the proposal row.

Idempotency is enforced by the database, not by application code. `razorpay_payment_id` carries a
unique constraint; a duplicate webhook delivery loses the insert race, raises
`DataIntegrityViolationException`, and is acknowledged with `200 OK` without applying the side
effect twice. A read-then-write check would let two concurrent deliveries both pass.

## Merchant Command Console & UI Architecture

RazorYield provides a light, high-trust executive dashboard designed for merchant decision-makers:

* **Interactive Clearance Simulator:**
  - Real-time yield curve morphing and live tracking dot as target discount is adjusted.
  - 1-click discount presets (`10%`, `15%`, `20%`, `25%`, `35%`) with dynamic fill slider.
  - Plain-English business metrics: **Sales Speed** (`items/day`), **Projected Cash**, **Profit Status** (`Safe` / `At Risk`), and **Payout** (`Direct`).
* **Sovereign Guilloché Canvas Engine:**
  - 60 FPS dual-harmonic mathematical canvas ribbons inspired by Swiss watch dials and sovereign currency engraving.
  - Interactive magnetic cursor deflection on a pure `#ffffff` background for zero eye-strain and maximum data clarity.
* **1-Click Razorpay Approval:**
  - Direct merchant approval triggers the Razorpay Payment Links API, updates inventory status, and generates copyable payment URLs instantly with zero page reloads.
* **Guardrails HUD:**
  - Real-time visual tracking of Margin Floor (`15%`), Daily Budget consumption, and Human Oversight triggers.

## Running it locally, with nothing installed

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

That is the whole setup. No PostgreSQL, no Redis, no Docker. Under the `local` profile,
[`EmbeddedServersConfig`](src/main/java/com/razoryield/local/EmbeddedServersConfig.java) starts a
**real PostgreSQL** and a **real Redis** as child processes — their binaries ship inside Maven
artifacts — and stops them when the app exits. Flyway migrates the embedded database on the way up.

They are the genuine servers, not stand-ins, so the CHECK constraint, the unique constraint on
`razorpay_payment_id` and the atomic `INCRBY` all behave exactly as they will in production.

## Running it against real servers

Requires **Java 21**, **Maven**, **PostgreSQL**, and **Redis**.

```bash
docker run -d --name razoryield-pg \
  -e POSTGRES_USER=razoryield -e POSTGRES_PASSWORD=razoryield -e POSTGRES_DB=razoryield \
  -p 5432:5432 postgres:17

docker run -d --name razoryield-redis -p 6379:6379 redis:7

export DB_URL=jdbc:postgresql://localhost:5432/razoryield
export DB_USERNAME=razoryield
export DB_PASSWORD=razoryield
export MERCHANT_API_KEY=mk_test_change_me
export RAZORPAY_KEY_ID=rzp_test_xxx
export RAZORPAY_KEY_SECRET=xxx
export RAZORPAY_WEBHOOK_SECRET=xxx
export OPENAI_API_KEY=sk-xxx

mvn spring-boot:run
```

Flyway applies `V1__init_schema.sql` and `V2__seed_data.sql` on startup. Hibernate is set to
`ddl-auto=validate`, so Flyway owns the schema and the entities are checked against it.

### Verifying the migrations

```sql
-- The margin-breach fixture: floor price already exceeds base price, so no discount can ever clear.
SELECT sku, cost_price_paise, base_price_paise,
       (cost_price_paise * 115) / 100 AS floor_price_paise
FROM products
WHERE (cost_price_paise * 115) / 100 > base_price_paise;
--  SKU-LEGACY-PRINTER | 95000 | 100000 | 109250

-- All five cohort rows must match the targeting criteria.
SELECT customer_id, phone_number, days_since_last_purchase, total_orders
FROM customer_cohorts
WHERE days_since_last_purchase >= 45 AND total_orders >= 2;
--  5 rows
```

## Tests

```bash
mvn test
```

45 tests, no live calls to OpenAI or Razorpay, and **no Docker required** — `FlywayMigrationTest`
starts a real PostgreSQL in-process and runs the actual migrations against it.

| Suite | What it pins down |
|-------|-------------------|
| `FlywayMigrationTest` | Migrations apply; every `%_paise` column is `BIGINT`; the margin-breach fixture is genuinely unservable; the status CHECK rejects invented states; the unique constraint rejects a duplicate payment id but still allows many null ones |
| `DiscountPolicyValidatorTest` | Margin breach short-circuits before Redis is touched; an over-budget reservation is rolled back; TTL is set only by the caller that created the key; the floor boundary is inclusive |
| `CampaignStateMachineServiceTest` | Auto-dispatch, human-approval and rejection paths; a low percentage on a large cash amount still needs approval; budget depletion is recorded under its own verdict |
| `InventoryAnalyzerAiServiceTest` | Valid JSON parses; malformed JSON, timeouts, empty responses, missing reasoning and non-positive prices all become `AiAnalysisFailedException` |
| `RazorpayGatewayServiceTest` | The payload carries paise and the right `reference_id`; `RazorpayException` is wrapped so no SDK type escapes |
| `CampaignApprovalControllerTest` | 200 on approval, 401 without a key, 409 from the wrong state with Razorpay never called, 404 unknown, 502 on gateway failure |
| `RazorpayWebhookControllerTest` | Forged, missing and tampered signatures all rejected before any DB access; a duplicate insert is acknowledged; a first delivery appends a `PAID` row |

## API

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/v1/campaigns/{id}/approve` | Merchant approval. Requires `X-Merchant-Key`. |
| `POST` | `/api/v1/webhooks/razorpay` | Settlement events. Requires `X-Razorpay-Signature`. |

## Known limitations

- Settlement depends entirely on Razorpay delivering the webhook. There is no CRON reconciliation
  job to sweep up payments whose notification never arrived. A production deployment needs one.
- The merchant gate is a single shared static key, not per-user authentication.
- The daily budget is a single global bucket (`merchant:default`), not per-merchant.
