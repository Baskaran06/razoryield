package com.razoryield.api;

/**
 * The failure modes the approval endpoint can produce, each mapped to a status code by
 * {@link ApiExceptionHandler}.
 */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** Missing or wrong X-Merchant-Key. */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    /** No campaign with that id. */
    public static class CampaignNotFoundException extends RuntimeException {
        public CampaignNotFoundException(String message) {
            super(message);
        }
    }

    /** The campaign exists but is not in a state that can be approved. */
    public static class InvalidCampaignStateException extends RuntimeException {
        public InvalidCampaignStateException(String message) {
            super(message);
        }
    }
}
