package com.razoryield.policy;

/**
 * Thrown when granting a discount would push the merchant past their daily discount budget.
 */
public class DailyBudgetDepletedException extends RuntimeException {

    private final long consumedPaise;
    private final long requestedDiscountPaise;
    private final long dailyCapPaise;

    public DailyBudgetDepletedException(long consumedPaise, long requestedDiscountPaise, long dailyCapPaise) {
        super(("Daily discount budget depleted: %d paise already consumed today, %d paise more requested, "
                + "against a daily cap of %d paise.")
                .formatted(consumedPaise, requestedDiscountPaise, dailyCapPaise));
        this.consumedPaise = consumedPaise;
        this.requestedDiscountPaise = requestedDiscountPaise;
        this.dailyCapPaise = dailyCapPaise;
    }

    public long getConsumedPaise() {
        return consumedPaise;
    }

    public long getRequestedDiscountPaise() {
        return requestedDiscountPaise;
    }

    public long getDailyCapPaise() {
        return dailyCapPaise;
    }
}
