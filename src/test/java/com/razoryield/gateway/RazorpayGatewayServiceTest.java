package com.razoryield.gateway;

import com.razorpay.PaymentLink;
import com.razorpay.PaymentLinkClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * No live calls to Razorpay. The SDK client is mocked and its public paymentLink field is populated
 * by reflection, which is the only way to intercept it.
 */
class RazorpayGatewayServiceTest {

    private PaymentLinkClient paymentLinkClient;
    private RazorpayGatewayService service;

    @BeforeEach
    void setUp() throws Exception {
        RazorpayClient razorpayClient = mock(RazorpayClient.class);
        paymentLinkClient = mock(PaymentLinkClient.class);

        Field field = RazorpayClient.class.getField("paymentLink");
        field.set(razorpayClient, paymentLinkClient);

        service = new RazorpayGatewayService("rzp_test_key", "secret");
        service.setRazorpayClient(razorpayClient);
    }

    @Test
    @DisplayName("A: a successful create returns the link id and sends the amount in paise")
    void createsPaymentLinkAndReturnsId() throws Exception {
        PaymentLink created = new PaymentLink(new JSONObject().put("id", "plink_test123"));
        when(paymentLinkClient.create(any(JSONObject.class))).thenReturn(created);

        String linkId = service.createPaymentLink(
                "3f1c9c1e-0b2a-4f9e-9d5a-2b7c1d8e4a10", "SKU-HEADPHONE-BT", 108_000L, "+919840421877");

        assertThat(linkId).isEqualTo("plink_test123");

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(paymentLinkClient).create(captor.capture());
        JSONObject payload = captor.getValue();

        assertThat(payload.getLong("amount")).isEqualTo(108_000L);
        assertThat(payload.getString("currency")).isEqualTo("INR");
        assertThat(payload.getBoolean("accept_partial")).isFalse();
        assertThat(payload.getString("description")).isEqualTo("RazorYield Flash Sale for SKU-HEADPHONE-BT");
        assertThat(payload.getString("reference_id")).isEqualTo("3f1c9c1e-0b2a-4f9e-9d5a-2b7c1d8e4a10");
        assertThat(payload.getJSONObject("customer").getString("contact")).isEqualTo("+919840421877");
    }

    @Test
    @DisplayName("B: an SDK failure is rethrown as PaymentGatewayException, keeping the original message")
    void wrapsRazorpayException() throws Exception {
        when(paymentLinkClient.create(any(JSONObject.class)))
                .thenThrow(new RazorpayException("Authentication failed"));

        assertThatThrownBy(() -> service.createPaymentLink(
                "3f1c9c1e-0b2a-4f9e-9d5a-2b7c1d8e4a10", "SKU-HEADPHONE-BT", 108_000L, "+919840421877"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("3f1c9c1e-0b2a-4f9e-9d5a-2b7c1d8e4a10")
                .hasMessageContaining("Authentication failed")
                .cause().isInstanceOf(RazorpayException.class);
    }

    @Test
    @DisplayName("an unconfigured client fails loudly instead of silently doing nothing")
    void failsWhenClientNotInitialised() {
        RazorpayGatewayService unconfigured = new RazorpayGatewayService("", "");
        unconfigured.initialiseClient();

        assertThatThrownBy(() -> unconfigured.createPaymentLink("id", "SKU", 1_000L, "+919840421877"))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("not initialised");
    }
}
