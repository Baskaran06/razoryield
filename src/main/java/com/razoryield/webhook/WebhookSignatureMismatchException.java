package com.razoryield.webhook;

/**
 * The payload did not carry a signature this merchant's webhook secret can vouch for.
 * Mapped to 401 by the shared exception handler.
 */
public class WebhookSignatureMismatchException extends RuntimeException {

    public WebhookSignatureMismatchException(String message) {
        super(message);
    }
}
