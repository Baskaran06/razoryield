# RazorYield — Autonomous Dead-Stock Liquidation Engine

> **Razorpay Hackathon Entry**  
> An autonomous, margin-guarded flash sale orchestrator that unlocks trapped capital in aging merchant inventory through dynamic clearance pricing, strict deterministic profit guardrails, and automated Razorpay Payment Links.

---

## ?? What is RazorYield?

Retail and D2C merchants frequently have **thousands of rupees tied up in idle, stagnant inventory**. Clearing this stock manually is slow, while blunt blanket discounts often eat into profit margins or trigger price wars.

**RazorYield solves this autonomously:**
1. **Scans Inventory**: Detects stagnant stock that has been idle for 45+ days.
2. **Dynamic Pricing**: Proposes dynamic clearance discounts modeled on item velocity and sales speed.
3. **Enforces Hard Guardrails**: Automatically enforces a strict **15% minimum margin floor** and daily discount budget cap before any action can occur.
4. **Direct Settlement via Razorpay**: Generates instant Razorpay Payment Links (`plink_xxx`) for 1-click customer checkout and direct merchant settlement.

---

## ??? Core Rule: "AI Proposes. Deterministic Code Disposes."

In financial systems, machine learning models should **never** have direct authority over money, payouts, or price commitments. 

RazorYield strictly isolates AI generation from financial execution:

```
+--------------------------------+
¦  AI Inventory Analyzer (LLM)   ¦ --> Generates Untrusted Proposal (Discount %, Reason)
+--------------------------------+
               ¦
               ?
+--------------------------------+
¦   Deterministic Policy Gate    ¦ --> Hard Math Check: Floor Price = (Cost × 1.15)
+--------------------------------+
               ¦
               ?
+--------------------------------+
¦     Atomic Redis Budget Gate   ¦ --> Atomic INCRBY: Daily cap of ?20,000
+--------------------------------+
               ¦
               ?
+--------------------------------+
¦    Human / Auto-Dispatch Gate  ¦ --> <= 10% & <= ?500: Auto | Above: Merchant Approval
+--------------------------------+
               ¦
               ?
+--------------------------------+
¦    Razorpay Gateway Service    ¦ --> Creates Live Payment Link (Direct Settlement)
+--------------------------------+
```

| Pipeline Stage | Component | AI Model Involved? |
| :--- | :--- | :---: |
| Propose discount & explain rationale | `InventoryAnalyzerAiService` | **Yes (Untrusted input)** |
| Enforce 15% margin floor | `DiscountPolicyValidator` | **No (Deterministic Math)** |
| Reserve against daily budget | `DiscountPolicyValidator` | **No (Atomic Redis)** |
| Determine auto-dispatch vs. approval | `CampaignStateMachineService` | **No (Deterministic Rules)** |
| Merchant 1-click approval | `CampaignApprovalController` | **No (Human Gate)** |
| Create Razorpay payment link | `RazorpayGatewayService` | **No (Razorpay API)** |
| Confirm webhook settlement | `RazorpayWebhookController` | **No (HMAC SHA-256)** |
| Append-only audit record | `AuditService` | **No (PostgreSQL)** |

---

## ?? Zero-Float Money Safety

To prevent floating-point rounding errors and precision drift in financial transactions:
* **100% Integer Arithmetic**: Every currency value is stored and calculated as an integer `long` of paise (1 Rupee = 100 Paise) in Java and PostgreSQL (`BIGINT`).
* **No `float`, `double`, or `BigDecimal` for Money**: The margin floor is computed using integer multiplication before division:
  ```java
  long floorPricePaise = (costPricePaise * (100L + globalMinMarginPct)) / 100L;
  ```
* `BigDecimal` is strictly reserved for `discount_pct`, which is a mathematical rate, not an amount of money.

---

## ?? The 3 Guardrail Gates

1. **Margin Floor Gate (15% Minimum)**:
   * Configured via `orchestrator.policy.global-min-margin-pct=15`.
   * Any discount that drops the offer price below `cost_price × 1.15` is immediately rejected (`REJECTED_MARGIN_BREACH`) before Redis or Razorpay are even contacted.

2. **Daily Budget Cap Gate (?20,000 / day)**:
   * Tracked atomically in Redis under `merchant:default:budget:yyyy-MM-dd` with a 24-hour TTL.
   * Uses an atomic `INCRBY` reservation with automatic negative compensation if an offer overshoots the cap, preventing race conditions under high concurrency.

3. **Human Oversight Gate**:
   * **Auto-Dispatched**: Offers $\le 10.00\%$ discount **and** $\le ?500$ cash savings dispatch automatically.
   * **Approval Required**: Any proposal exceeding either limit pauses at `PENDING_MERCHANT_APPROVAL` for 1-click merchant authorization holding a valid `X-Merchant-Key`.

---

## ??? Executive Merchant Console & UI Architecture

RazorYield features a modern, executive dashboard designed for merchant decision-makers:

* **Interactive Clearance Simulator**:
  * Real-time dynamic SVG yield curve showing the balance between discount rate and clearance velocity.
  * 1-click preset chips (`10%`, `15%`, `20%`, `25%`, `35%`) with dynamic slider track coloring.
  * Real-time business metrics: **Sales Speed** (`items/day`), **Projected Cash**, **Profit Status** (`Safe` / `At Risk`), and **Payout Mode**.
* **Sovereign Guilloché Canvas Engine**:
  * 60 FPS mathematical dual-harmonic canvas ribbons inspired by Swiss watch dials and sovereign currency engraving.
  * Interactive magnetic cursor deflection on a pure white `#ffffff` backdrop with zero text interference.
* **Interactive Campaigns Workspace**:
  * Real-time filter tabs with dynamic counts: **`All Campaigns`**, **`Active & Selling`**, **`Pending Approval`**, and **`Margin Guarded`**.
  * Clickable Razorpay checkout links with 1-click clipboard copying.
  * **Campaign Inspector Modal**: Clicking any campaign row opens a detailed breakdown of product metrics, AI reasoning, and direct Razorpay checkout links.
* **Guardrails HUD**:
  * Real-time visual monitoring of Margin Protection, Daily Budget utilization, and Active Clearance velocity.

---

## ?? Quickstart: Running Locally (Zero Dependencies)

The application includes embedded PostgreSQL and embedded Redis configurations that run in-memory inside Maven — **you do NOT need to install Docker, PostgreSQL, or Redis to test this project**.

### Prerequisites
* **Java 21** installed (`java -version`)

### One-Command Launch

**On Windows (PowerShell / Command Prompt):**
```powershell
git clone https://github.com/Baskaran06/razoryield.git
cd razoryield/razoryield
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

**On macOS / Linux:**
```bash
git clone https://github.com/Baskaran06/razoryield.git
cd razoryield/razoryield
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Open the Application
Once started, navigate to:
?? **[http://localhost:8080/](http://localhost:8080/)**

Flyway automatically seeds the database with initial inventory, customer cohorts, and sample campaigns.

---

## ?? Running with Production Services (Docker)

To run RazorYield against real external PostgreSQL and Redis instances:

```bash
# 1. Start Postgres & Redis containers
docker run -d --name razoryield-pg \
  -e POSTGRES_USER=razoryield -e POSTGRES_PASSWORD=razoryield -e POSTGRES_DB=razoryield \
  -p 5432:5432 postgres:17

docker run -d --name razoryield-redis -p 6379:6379 redis:7

# 2. Export environment variables
export DB_URL=jdbc:postgresql://localhost:5432/razoryield
export DB_USERNAME=razoryield
export DB_PASSWORD=razoryield
export MERCHANT_API_KEY=mk_test_secret_key
export RAZORPAY_KEY_ID=rzp_test_your_id
export RAZORPAY_KEY_SECRET=your_secret
export RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

# 3. Launch with standard profile
./mvnw spring-boot:run
```

---

## ?? Automated Tests

The test suite contains **45 automated unit and integration tests** verifying policy math, database migrations, state transitions, and webhook idempotency without requiring Docker or live API keys:

```bash
./mvnw test
```

| Test Suite | Coverage & Guarantees |
| :--- | :--- |
| `FlywayMigrationTest` | Verifies Flyway migrations, PostgreSQL `BIGINT` schema types, and CHECK constraints. |
| `DiscountPolicyValidatorTest` | Validates 15% margin floor math, atomic Redis budget caps, and compensation rollbacks. |
| `CampaignStateMachineServiceTest` | Verifies auto-dispatch transitions, human approval gates, and state invariants. |
| `InventoryAnalyzerAiServiceTest` | Tests AI JSON parsing, fallback triggers, and malformed payload recovery. |
| `RazorpayGatewayServiceTest` | Verifies Razorpay payment link payload structures and error wrapping. |
| `RazorpayWebhookControllerTest` | Asserts HMAC SHA-256 signature verification and database idempotency against duplicate deliveries. |
| `CampaignApprovalControllerTest` | Tests 1-click merchant approvals, unauthorized access checks, and state conflict handling. |

---

## ?? API Reference

| Method | Endpoint | Description | Headers |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Executive Merchant Dashboard & Clearance Simulator | — |
| `GET` | `/campaigns` | Interactive Clearance Campaigns Workspace | — |
| `POST` | `/orchestrate` | Trigger inventory scan & campaign proposal cycle | — |
| `POST` | `/api/v1/campaigns/{id}/approve` | Merchant 1-click campaign approval | `X-Merchant-Key: <key>` |
| `POST` | `/api/v1/webhooks/razorpay` | Razorpay payment & settlement webhook | `X-Razorpay-Signature: <hmac>` |
| `GET` | `/api/v1/campaigns` | JSON list of all clearance campaigns | — |
| `GET` | `/api/v1/audit` | JSON append-only audit trail entries | — |

---

## ?? License

Built for the **Razorpay Hackathon**. Licensed under the [MIT License](LICENSE).
