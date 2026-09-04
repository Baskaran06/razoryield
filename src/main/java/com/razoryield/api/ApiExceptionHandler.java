package com.razoryield.api;

import com.razoryield.gateway.PaymentGatewayException;
import com.razoryield.policy.DailyBudgetDepletedException;
import com.razoryield.policy.MarginFloorBreachedException;
import com.razoryield.webhook.WebhookProcessingException;
import com.razoryield.webhook.WebhookSignatureMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message));
    }

    @ExceptionHandler(ApiExceptions.UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> onUnauthorized(ApiExceptions.UnauthorizedException e) {
        return body(HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage());
    }

    @ExceptionHandler(ApiExceptions.CampaignNotFoundException.class)
    public ResponseEntity<Map<String, Object>> onNotFound(ApiExceptions.CampaignNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());
    }

    @ExceptionHandler(ApiExceptions.InvalidCampaignStateException.class)
    public ResponseEntity<Map<String, Object>> onConflict(ApiExceptions.InvalidCampaignStateException e) {
        return body(HttpStatus.CONFLICT, "Conflict", e.getMessage());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<Map<String, Object>> onGatewayFailure(PaymentGatewayException e) {
        log.error("Payment gateway failure", e);
        return body(HttpStatus.BAD_GATEWAY, "Bad Gateway", e.getMessage());
    }

    @ExceptionHandler(MarginFloorBreachedException.class)
    public ResponseEntity<Map<String, Object>> onMarginBreach(MarginFloorBreachedException e) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "Margin Floor Breached", e.getMessage());
    }

    @ExceptionHandler(DailyBudgetDepletedException.class)
    public ResponseEntity<Map<String, Object>> onBudgetDepleted(DailyBudgetDepletedException e) {
        return body(HttpStatus.TOO_MANY_REQUESTS, "Daily Budget Depleted", e.getMessage());
    }

    @ExceptionHandler(WebhookSignatureMismatchException.class)
    public ResponseEntity<Map<String, Object>> onBadSignature(WebhookSignatureMismatchException e) {
        return body(HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage());
    }

    @ExceptionHandler(WebhookProcessingException.class)
    public ResponseEntity<Map<String, Object>> onBadWebhookPayload(WebhookProcessingException e) {
        return body(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());
    }
}
