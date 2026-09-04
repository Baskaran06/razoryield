package com.razoryield.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DiscountPolicyValidatorTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private DiscountPolicyValidator validator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        validator = new DiscountPolicyValidator(redis, 15);
    }

    @Test
    @DisplayName("A: the margin-breach fixture is rejected before Redis is ever touched")
    void marginFloorBreachShortCircuitsBeforeRedis() {
        long costPricePaise = 95_000L;
        long basePricePaise = 100_000L;
        long offerPricePaise = 90_000L;

        assertThatThrownBy(() -> validator.validate("SKU-LEGACY-PRINTER", costPricePaise, basePricePaise, offerPricePaise))
                .isInstanceOf(MarginFloorBreachedException.class)
                .hasMessageContaining("SKU-LEGACY-PRINTER")
                .hasMessageContaining("109250")   // 95000 * 115 / 100
                .hasMessageContaining("90000");

        verifyNoInteractions(valueOps);
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("B: an over-budget reservation is rolled back and reported")
    void dailyBudgetDepletedRollsBackReservation() {
        long costPricePaise = 45_000L;
        long basePricePaise = 120_000L;
        long offerPricePaise = 90_000L;
        long discountPaise = basePricePaise - offerPricePaise;   // 30,000 paise

        // Reserving pushes the running total past the 2,000,000 paise cap.
        when(valueOps.increment(anyString(), eq(discountPaise))).thenReturn(2_010_000L);

        assertThatThrownBy(() -> validator.validate("SKU-HEADPHONE-BT", costPricePaise, basePricePaise, offerPricePaise))
                .isInstanceOf(DailyBudgetDepletedException.class)
                .hasMessageContaining("1980000")   // consumed before this request
                .hasMessageContaining("30000")
                .hasMessageContaining("2000000");

        String key = validator.budgetKeyForToday();
        verify(valueOps).increment(key, discountPaise);
        verify(valueOps).increment(key, -discountPaise);
        verify(valueOps, times(2)).increment(anyString(), anyLong());
    }

    @Test
    @DisplayName("C: a healthy discount inside budget passes cleanly")
    void happyPathPasses() {
        long costPricePaise = 45_000L;
        long basePricePaise = 120_000L;
        long offerPricePaise = 90_000L;
        long discountPaise = 30_000L;

        when(valueOps.increment(anyString(), eq(discountPaise))).thenReturn(450_000L);

        validator.validate("SKU-HEADPHONE-BT", costPricePaise, basePricePaise, offerPricePaise);

        verify(valueOps).increment(validator.budgetKeyForToday(), discountPaise);
        verify(valueOps, never()).increment(anyString(), eq(-discountPaise));
    }

    @Test
    @DisplayName("the TTL is set only by the caller that created today's key")
    void ttlAppliedOnlyOnFirstReservation() {
        when(valueOps.increment(anyString(), eq(30_000L))).thenReturn(30_000L);

        validator.validate("SKU-HEADPHONE-BT", 45_000L, 120_000L, 90_000L);

        verify(redis).expire(validator.budgetKeyForToday(), Duration.ofHours(24));
    }

    @Test
    @DisplayName("a later reservation on the same day does not extend the TTL")
    void ttlNotReappliedOnSubsequentReservations() {
        when(valueOps.increment(anyString(), eq(30_000L))).thenReturn(120_000L);

        validator.validate("SKU-HEADPHONE-BT", 45_000L, 120_000L, 90_000L);

        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("an offer exactly on the floor is allowed, one paise under is not")
    void floorBoundaryIsInclusive() {
        long costPricePaise = 45_000L;
        long floor = validator.floorPricePaise(costPricePaise);
        assertThat(floor).isEqualTo(51_750L);

        when(valueOps.increment(anyString(), anyLong())).thenReturn(100_000L);
        validator.validate("SKU-HEADPHONE-BT", costPricePaise, 120_000L, floor);

        assertThatThrownBy(() -> validator.validate("SKU-HEADPHONE-BT", costPricePaise, 120_000L, floor - 1))
                .isInstanceOf(MarginFloorBreachedException.class);
    }

    @Test
    @DisplayName("a zero discount reserves nothing")
    void zeroDiscountSkipsBudget() {
        validator.validate("SKU-TEA-250G", 12_000L, 24_000L, 24_000L);

        verifyNoInteractions(valueOps);
    }
}
