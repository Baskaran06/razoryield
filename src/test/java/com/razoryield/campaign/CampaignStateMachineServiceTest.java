package com.razoryield.campaign;

import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.domain.CampaignStatus;
import com.razoryield.policy.DailyBudgetDepletedException;
import com.razoryield.policy.DiscountPolicyValidator;
import com.razoryield.policy.MarginFloorBreachedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignStateMachineServiceTest {

    private CampaignRepository campaignRepository;
    private AuditService auditService;
    private DiscountPolicyValidator validator;
    private CampaignStateMachineService service;

    @BeforeEach
    void setUp() {
        campaignRepository = mock(CampaignRepository.class);
        auditService = mock(AuditService.class);
        validator = mock(DiscountPolicyValidator.class);
        service = new CampaignStateMachineService(campaignRepository, auditService, validator);

        // The repository echoes back whatever it was asked to save.
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CampaignAuditLog capturedAudit() {
        ArgumentCaptor<CampaignAuditLog> captor = ArgumentCaptor.forClass(CampaignAuditLog.class);
        verify(auditService).append(captor.capture());
        return captor.getValue();
    }

    private Campaign capturedCampaign() {
        ArgumentCaptor<Campaign> captor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("A: a small discount inside both auto-dispatch limits dispatches without a human")
    void autoDispatchesSmallDiscount() {
        // 5% off, Rs.200 (20,000 paise) of cash discount.
        service.evaluateAndPersist("SKU-RICE-10KG", 52_000L, 400_000L,
                new BigDecimal("5.00"), 380_000L, "Idle 21 days with healthy stock; a light nudge should clear it.");

        verify(validator).validate("SKU-RICE-10KG", 52_000L, 400_000L, 380_000L);
        assertThat(capturedCampaign().getStatus()).isEqualTo(CampaignStatus.AUTO_DISPATCHED);

        CampaignAuditLog audit = capturedAudit();
        assertThat(audit.getGateVerdict()).isEqualTo(CampaignStateMachineService.VERDICT_AUTO_DISPATCH);
        assertThat(audit.getLlmReasoning()).contains("Idle 21 days");
        assertThat(audit.getCostPricePaise()).isEqualTo(52_000L);
        assertThat(audit.getOfferPricePaise()).isEqualTo(380_000L);
        assertThat(audit.getSettlementStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("B: a margin breach is recorded and rethrown, and never becomes dispatchable")
    void marginBreachIsRecordedAndRethrown() {
        doThrow(new MarginFloorBreachedException("SKU-LEGACY-PRINTER", 109_250L, 90_000L))
                .when(validator).validate(anyString(), anyLong(), anyLong(), anyLong());

        assertThatThrownBy(() -> service.evaluateAndPersist("SKU-LEGACY-PRINTER", 95_000L, 100_000L,
                new BigDecimal("10.00"), 90_000L, "Dead stock for 214 days, clear it at any price."))
                .isInstanceOf(MarginFloorBreachedException.class);

        assertThat(capturedCampaign().getStatus()).isEqualTo(CampaignStatus.REJECTED_MARGIN_BREACH);

        CampaignAuditLog audit = capturedAudit();
        assertThat(audit.getGateVerdict()).isEqualTo(CampaignStateMachineService.VERDICT_MARGIN_BREACH);
        assertThat(audit.getFailureReason()).contains("Margin floor breached");
        assertThat(audit.getLlmReasoning()).contains("214 days");
    }

    @Test
    @DisplayName("C: a discount past either auto-dispatch limit waits for a merchant")
    void largeDiscountRequiresHumanApproval() {
        // 25% off and Rs.1,000 of cash discount: past both thresholds.
        service.evaluateAndPersist("SKU-KETTLE-1L", 90_000L, 400_000L,
                new BigDecimal("25.00"), 300_000L, "Idle 103 days; needs a deep cut to move.");

        assertThat(capturedCampaign().getStatus()).isEqualTo(CampaignStatus.PENDING_MERCHANT_APPROVAL);
        assertThat(capturedAudit().getGateVerdict())
                .isEqualTo(CampaignStateMachineService.VERDICT_REQUIRES_APPROVAL);
    }

    @Test
    @DisplayName("a low rate on a large cash amount still needs approval")
    void lowPctButLargeCashDiscountRequiresApproval() {
        // 4% off, but Rs.800 (80,000 paise) in cash, over the 50,000 paise ceiling.
        service.evaluateAndPersist("SKU-KETTLE-1L", 90_000L, 2_000_000L,
                new BigDecimal("4.00"), 1_920_000L, "High ticket item, small percentage.");

        assertThat(capturedCampaign().getStatus()).isEqualTo(CampaignStatus.PENDING_MERCHANT_APPROVAL);
    }

    @Test
    @DisplayName("a depleted budget is recorded with its own verdict, distinct from a margin breach")
    void budgetDepletionIsRecordedSeparately() {
        doThrow(new DailyBudgetDepletedException(1_980_000L, 30_000L, 2_000_000L))
                .when(validator).validate(anyString(), anyLong(), anyLong(), anyLong());

        assertThatThrownBy(() -> service.evaluateAndPersist("SKU-HEADPHONE-BT", 45_000L, 120_000L,
                new BigDecimal("25.00"), 90_000L, "Lapsed buyers respond to this SKU."))
                .isInstanceOf(DailyBudgetDepletedException.class);

        assertThat(capturedAudit().getGateVerdict())
                .isEqualTo(CampaignStateMachineService.VERDICT_BUDGET_DEPLETED);
    }

    @Test
    @DisplayName("the validator runs before anything is persisted")
    void nothingIsPersistedWhenTheGateRefuses() {
        doThrow(new MarginFloorBreachedException("SKU-LEGACY-PRINTER", 109_250L, 90_000L))
                .when(validator).validate(anyString(), anyLong(), anyLong(), anyLong());

        assertThatThrownBy(() -> service.evaluateAndPersist("SKU-LEGACY-PRINTER", 95_000L, 100_000L,
                new BigDecimal("10.00"), 90_000L, "reasoning"))
                .isInstanceOf(MarginFloorBreachedException.class);

        // Exactly one campaign row, and it is the rejection. No dispatchable campaign was written.
        verify(campaignRepository, never()).save(
                org.mockito.ArgumentMatchers.argThat(c -> c.getStatus() == CampaignStatus.AUTO_DISPATCHED));
    }

    @Test
    @DisplayName("a discount exactly on both auto-dispatch boundaries still auto-dispatches")
    void boundaryValuesAutoDispatch() {
        // Exactly 10.00% and exactly 50,000 paise.
        service.evaluateAndPersist("SKU-BACKPACK-30L", 70_000L, 500_000L,
                new BigDecimal("10.00"), 450_000L, "Boundary case.");

        assertThat(capturedCampaign().getStatus()).isEqualTo(CampaignStatus.AUTO_DISPATCHED);
    }
}
