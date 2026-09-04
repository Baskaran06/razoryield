package com.razoryield.ai;

/**
 * Raised when the model times out, returns nothing, or returns something that will not parse into
 * an {@link LlmDiscountProposal}. Callers treat this as "no proposal", never as "proceed anyway".
 */
public class AiAnalysisFailedException extends RuntimeException {

    public AiAnalysisFailedException(String message) {
        super(message);
    }

    public AiAnalysisFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
