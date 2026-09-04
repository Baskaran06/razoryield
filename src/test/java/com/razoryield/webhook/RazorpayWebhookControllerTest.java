package com.razoryield.webhook;

import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignAuditLogRepository;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.domain.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RazorpayWebhookController.class)
@TestPropertySource(properties = "razorpay.webhook.secret=" + RazorpayWebhookControllerTest.WEBHOOK_SECRET)
class RazorpayWebhookControllerTest {

    static final String WEBHOOK_SECRET = "whsec_test_9f2c";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CampaignRepository campaignRepository;

    @MockitoBean
    CampaignAuditLogRepository auditLogRepository;

    private UUID campaignId;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        Campaign campaign = new Campaign("SKU-KETTLE-1L", CampaignStatus.APPROVED, new BigDecimal("25.00"), 300_000L);
        campaign.setRazorpayLinkId("plink_test123");
        // A campaign read back from Postgres always carries its generated id; the constructor does not.
        ReflectionTestUtils.setField(campaign, "id", campaignId);
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
    }

    /** Razorpay signs the raw body with HMAC-SHA256 using the webhook secret. */
    private static String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private String paidEvent(String paymentId) {
        return """
                {"event":"payment_link.paid","payload":{"payment_link":{"entity":{"id":"plink_test123","reference_id":"%s"}},"payment":{"entity":{"id":"%s"}}}}"""
                .formatted(campaignId, paymentId);
    }

    @Test
    @DisplayName("A: an invalid signature is refused and the database is never touched")
    void invalidSignatureIsUnauthorized() throws Exception {
        String body = paidEvent("pay_abc123");

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "0".repeat(64))
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campaignRepository, auditLogRepository);
    }

    @Test
    @DisplayName("A2: a missing signature header is refused")
    void missingSignatureIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paidEvent("pay_abc123")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campaignRepository, auditLogRepository);
    }

    @Test
    @DisplayName("B: a duplicate delivery is absorbed by the unique constraint and acknowledged")
    void duplicateDeliveryIsAcknowledged() throws Exception {
        String body = paidEvent("pay_abc123");
        when(auditLogRepository.saveAndFlush(any(CampaignAuditLog.class)))
                .thenThrow(new DataIntegrityViolationException("unique_payment_id violated"));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(body))
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C: a first delivery appends a new PAID row rather than updating an existing one")
    void successAppendsSettlementRow() throws Exception {
        String body = paidEvent("pay_abc123");
        when(auditLogRepository.saveAndFlush(any(CampaignAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(body))
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<CampaignAuditLog> captor = ArgumentCaptor.forClass(CampaignAuditLog.class);
        verify(auditLogRepository).saveAndFlush(captor.capture());
        CampaignAuditLog row = captor.getValue();

        assertThat(row.getSettlementStatus()).isEqualTo("PAID");
        assertThat(row.getGateVerdict()).isEqualTo("WEBHOOK_SETTLED");
        assertThat(row.getRazorpayPaymentId()).isEqualTo("pay_abc123");
        assertThat(row.getRazorpayLinkId()).isEqualTo("plink_test123");
        assertThat(row.getCampaignId()).isEqualTo(campaignId);

        // Append-only: no existing row was mutated.
        verify(auditLogRepository, never()).delete(any());
    }

    @Test
    @DisplayName("an unrelated event is acknowledged and ignored")
    void unrelatedEventIsIgnored() throws Exception {
        String body = """
                {"event":"payment.authorized","payload":{"payment":{"entity":{"id":"pay_zzz"}}}}""";

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(body))
                        .content(body))
                .andExpect(status().isOk());

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("a tampered body no longer matches its signature")
    void tamperedBodyIsUnauthorized() throws Exception {
        String signature = sign(paidEvent("pay_abc123"));
        String tampered = paidEvent("pay_attacker_controlled");

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(tampered))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("a signed event for an unknown campaign is acknowledged without writing")
    void unknownCampaignIsAcknowledged() throws Exception {
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());
        String body = paidEvent("pay_abc123");

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(body))
                        .content(body))
                .andExpect(status().isOk());

        verifyNoInteractions(auditLogRepository);
    }
}
