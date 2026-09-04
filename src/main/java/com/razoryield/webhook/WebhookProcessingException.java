package com.razoryield.webhook;

/**
 * The signature verified but the body was not shaped the way the event contract says.
 * Mapped to 400 by the shared exception handler.
 */
public class WebhookProcessingException extends RuntimeException {

    public WebhookProcessingException(String message) {
        super(message);
    }

    public WebhookProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
