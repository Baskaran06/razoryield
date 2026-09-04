package com.razoryield.api;

import com.razoryield.campaign.AuditService;
import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.domain.CampaignStatus;
import com.razoryield.domain.CustomerCohortRepository;
import com.razoryield.domain.ProductRepository;
import com.razoryield.gateway.PaymentGatewayException;
import com.razoryield.gateway.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CampaignApprovalController.class)
@TestPropertySource(properties = "merchant.api.key=" + CampaignApprovalControllerTest.MERCHANT_KEY)
class CampaignApprovalControllerTest {

    static final String MERCHANT_KEY = "mk_test_super_secret";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CampaignRepository campaignRepository;

    @MockitoBean
    AuditService auditService;

    @MockitoBean
    PaymentGateway paymentGateway;

    @MockitoBean
    ProductRepository productRepository;

    @MockitoBean
    CustomerCohortRepository cohortRepository;

    private UUID campaignId;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        when(cohortRepository.findTargetable()).thenReturn(List.of());
        when(productRepository.findById(anyString())).thenReturn(Optional.empty());
    }

    private void givenCampaignIn(CampaignStatus status) {
        Campaign campaign = new Campaign("SKU-KETTLE-1L", status, new BigDecimal("25.00"), 300_000L);
        // A campaign read back from Postgres always carries its generated id; the constructor does not.
        ReflectionTestUtils.setField(campaign, "id", campaignId);
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("A: a valid key on a pending campaign dispatches a payment link")
    void approvesPendingCampaign() throws Exception {
        givenCampaignIn(CampaignStatus.PENDING_MERCHANT_APPROVAL);
        when(paymentGateway.createPaymentLink(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn("plink_test123");

        mockMvc.perform(post("/api/v1/campaigns/{id}/approve", campaignId)
                        .header("X-Merchant-Key", MERCHANT_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.razorpayLinkId").value("plink_test123"))
                .andExpect(jsonPath("$.offerPricePaise").value(300_000L));

        verify(paymentGateway).createPaymentLink(campaignId.toString(), "SKU-KETTLE-1L", 300_000L, "+910000000000");

        ArgumentCaptor<CampaignAuditLog> captor = ArgumentCaptor.forClass(CampaignAuditLog.class);
        verify(auditService).append(captor.capture());
        CampaignAuditLog audit = captor.getValue();
        assertThat(audit.getGateVerdict()).isEqualTo("MANUALLY_APPROVED");
        assertThat(audit.getApproverUserId()).isEqualTo("MERCHANT_ADMIN");
        assertThat(audit.getRazorpayLinkId()).isEqualTo("plink_test123");
    }

    @Test
    @DisplayName("B: a missing key is refused and never reaches the database")
    void rejectsMissingKey() throws Exception {
        mockMvc.perform(post("/api/v1/campaigns/{id}/approve", campaignId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campaignRepository, paymentGateway, auditService);
    }

    @Test
    @DisplayName("B2: a wrong key is refused")
    void rejectsWrongKey() throws Exception {
        mockMvc.perform(post("/api/v1/campaigns/{id}/approve", campaignId)
                        .header("X-Merchant-Key", "mk_test_wrong"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campaignRepository, paymentGateway, auditService);
    }

    @Test
    @DisplayName("C: an already-dispatched campaign conflicts and never reaches Razorpay")
    void rejectsCampaignInWrongState() throws Exception {
        givenCampaignIn(CampaignStatus.AUTO_DISPATCHED);

        mockMvc.perform(post("/api/v1/campaigns/{id}/approve", campaignId)
                        .header("X-Merchant-Key", MERCHANT_KEY))
                .andExpect(status().isConflict());

        verifyNoInteractions(paymentGateway);
        verify(auditService, never()).append(any());
    }

    @Test
    @DisplayName("C2: a rejected campaign cannot be approved back into life")
    void rejectsMarginBreachedCampaign() throws Exception {
        givenCampaignIn(CampaignStatus.REJECTED_MARGIN_BREACH);

        mockMvc.perform(post("/api/v1/campaigns/{id}/approve", campaignId)
                        .header("X-Merchant-Key", MERCHANT_KEY))
                .andExpect(status().isConflict());

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("an unknown campaign is a 404")
    void unknownCampaignIsNotFound() throws Exception {
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/campaigns/{id}/approve", campaignId)
                        .header("X-Merchant-Key", MERCHANT_KEY))
                .andExpect(status().isNotFound());

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("a gateway failure surfaces as 502 and leaves no approval audit row")
    void gatewayFailureIsBadGateway() throws Exception {
        givenCampaignIn(CampaignStatus.PENDING_MERCHANT_APPROVAL);
        when(paymentGateway.createPaymentLink(anyString(), anyString(), anyLong(), anyString()))
                .thenThrow(new PaymentGatewayException("Razorpay rejected the payment link: Authentication failed"));

        mockMvc.perform(post("/api/v1/campaigns/{id}/approve", campaignId)
                        .header("X-Merchant-Key", MERCHANT_KEY))
                .andExpect(status().isBadGateway());

        verify(auditService, never()).append(any());
    }
}
