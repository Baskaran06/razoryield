package com.razoryield.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentGatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayConfig.class);

    @Bean
    public PaymentGateway paymentGateway(@Value("${razorpay.api.key:}") String apiKey,
                                         @Value("${razorpay.api.secret:}") String apiSecret) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            log.warn("Razorpay credentials absent. Approved campaigns will use the simulated gateway.");
            return new SimulatedPaymentGateway();
        }
        RazorpayGatewayService gateway = new RazorpayGatewayService(apiKey, apiSecret);
        gateway.initialiseClient();
        return gateway;
    }
}
