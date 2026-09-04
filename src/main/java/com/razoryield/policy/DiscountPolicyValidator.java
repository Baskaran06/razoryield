package com.razoryield.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The deterministic gate in front of every discount. Nothing here consults a model: it is pure
 * integer arithmetic against the margin floor, plus an atomic Redis reservation against the
 * merchant's daily discount budget.
 */
@Service
public class DiscountPolicyValidator {

    private static final Logger log = LoggerFactory.getLogger(DiscountPolicyValidator.class);

    /** Rs.20,000 expressed in paise. */
    public static final long DAILY_BUDGET_CAP_PAISE = 2_000_000L;

    private static final String BUDGET_KEY_PREFIX = "merchant:default:budget:";
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId BUDGET_ZONE = ZoneId.of("Asia/Kolkata");
    private static final Duration BUDGET_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final int globalMinMarginPct;

    public DiscountPolicyValidator(StringRedisTemplate stringRedisTemplate,
                                   @Value("${orchestrator.policy.global-min-margin-pct:15}") int globalMinMarginPct) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.globalMinMarginPct = globalMinMarginPct;
    }

    /**
     * Rejects the discount unless it clears the margin floor and fits inside today's remaining budget.
     * On success the discount amount has been reserved in Redis.
     *
     * @throws MarginFloorBreachedException if the offer price would sell below the margin floor
     * @throws DailyBudgetDepletedException if the discount would exceed the daily budget cap
     */
    public void validate(String sku, long costPricePaise, long basePricePaise, long offerPricePaise) {
        enforceMarginFloor(sku, costPricePaise, offerPricePaise);
        reserveDailyBudget(sku, basePricePaise, offerPricePaise);
    }

    /**
     * Integer-only floor calculation. Multiplying before dividing keeps the whole expression in
     * long arithmetic, so no value is ever promoted to a floating-point type.
     */
    public long floorPricePaise(long costPricePaise) {
        return (costPricePaise * (100L + globalMinMarginPct)) / 100L;
    }

    private void enforceMarginFloor(String sku, long costPricePaise, long offerPricePaise) {
        long floorPricePaise = floorPricePaise(costPricePaise);
        if (offerPricePaise < floorPricePaise) {
            log.warn("Margin floor breached for {}: floor {} paise, offer {} paise", sku, floorPricePaise, offerPricePaise);
            throw new MarginFloorBreachedException(sku, floorPricePaise, offerPricePaise);
        }
    }

    /**
     * Reserves the discount against today's budget with a single atomic INCRBY, then rolls the
     * reservation back if it turned out to overshoot. Reserving first and compensating second is
     * what makes this safe under concurrency: two callers cannot both read a stale total and both
     * conclude there is room.
     */
    private void reserveDailyBudget(String sku, long basePricePaise, long offerPricePaise) {
        long discountPaise = basePricePaise - offerPricePaise;
        if (discountPaise <= 0) {
            return;
        }

        String budgetKey = budgetKeyForToday();
        Long newTotal = stringRedisTemplate.opsForValue().increment(budgetKey, discountPaise);

        if (newTotal == null) {
            throw new DailyBudgetDepletedException(0L, discountPaise, DAILY_BUDGET_CAP_PAISE);
        }

        // Only the caller that created the key sets the expiry, so the window never slides forward.
        if (newTotal == discountPaise) {
            stringRedisTemplate.expire(budgetKey, BUDGET_TTL);
        }

        if (newTotal > DAILY_BUDGET_CAP_PAISE) {
            stringRedisTemplate.opsForValue().increment(budgetKey, -discountPaise);
            long consumedBefore = newTotal - discountPaise;
            log.warn("Daily budget depleted while pricing {}: {} consumed, {} requested, cap {}",
                    sku, consumedBefore, discountPaise, DAILY_BUDGET_CAP_PAISE);
            throw new DailyBudgetDepletedException(consumedBefore, discountPaise, DAILY_BUDGET_CAP_PAISE);
        }

        log.info("Reserved {} paise of discount budget for {}. Today's total is now {} of {} paise.",
                discountPaise, sku, newTotal, DAILY_BUDGET_CAP_PAISE);
    }

    String budgetKeyForToday() {
        return BUDGET_KEY_PREFIX + LocalDate.now(BUDGET_ZONE).format(DAY_FORMAT);
    }
}
