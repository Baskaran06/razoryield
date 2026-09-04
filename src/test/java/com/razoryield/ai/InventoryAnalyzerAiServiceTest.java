package com.razoryield.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * No live API calls. The fluent ChatClient chain is stubbed so the service's parsing and its
 * failure handling are what get exercised.
 */
class InventoryAnalyzerAiServiceTest {

    private ChatClient.CallResponseSpec responseSpec;
    private InventoryAnalyzerAiService service;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        service = new InventoryAnalyzerAiService(builder);
    }

    private LlmDiscountProposal propose() {
        return service.analyzeAndPropose("SKU-HEADPHONE-BT", 45_000L, 120_000L, 57, 78, 62, 14);
    }

    @Test
    @DisplayName("A: a well-formed response parses into the proposal record")
    void parsesSuccessfulResponse() {
        LlmDiscountProposal expected = new LlmDiscountProposal(
                new BigDecimal("10.00"),
                108_000L,
                "Idle for 57 days with 78 units on hand, and the cohort has not bought in 62 days. "
                        + "A 10% cut to 108000 paise is enough to restart movement without giving away margin.");
        when(responseSpec.entity(eq(LlmDiscountProposal.class))).thenReturn(expected);

        LlmDiscountProposal actual = propose();

        assertThat(actual.discountPct()).isEqualByComparingTo("10.00");
        assertThat(actual.offerPricePaise()).isEqualTo(108_000L);
        assertThat(actual.llmReasoning()).contains("57 days");
    }

    @Test
    @DisplayName("B: a parse failure surfaces as AiAnalysisFailedException, not the raw cause")
    void wrapsParsingFailure() {
        when(responseSpec.entity(eq(LlmDiscountProposal.class)))
                .thenThrow(new RuntimeException("JSON parse error: unexpected token 'Sure, here is'"));

        assertThatThrownBy(this::propose)
                .isInstanceOf(AiAnalysisFailedException.class)
                .hasMessageContaining("SKU-HEADPHONE-BT")
                .hasMessageContaining("JSON parse error")
                .cause().isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("a timeout surfaces the same way")
    void wrapsTimeout() {
        when(responseSpec.entity(eq(LlmDiscountProposal.class)))
                .thenThrow(new RuntimeException("Read timed out"));

        assertThatThrownBy(this::propose)
                .isInstanceOf(AiAnalysisFailedException.class)
                .hasMessageContaining("Read timed out");
    }

    @Test
    @DisplayName("an empty response is rejected rather than treated as a zero discount")
    void rejectsNullResponse() {
        when(responseSpec.entity(eq(LlmDiscountProposal.class))).thenReturn(null);

        assertThatThrownBy(this::propose)
                .isInstanceOf(AiAnalysisFailedException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    @DisplayName("a proposal missing its reasoning is rejected, because the audit row requires it")
    void rejectsMissingReasoning() {
        when(responseSpec.entity(eq(LlmDiscountProposal.class)))
                .thenReturn(new LlmDiscountProposal(new BigDecimal("10.00"), 108_000L, "   "));

        assertThatThrownBy(this::propose)
                .isInstanceOf(AiAnalysisFailedException.class)
                .hasMessageContaining("no reasoning");
    }

    @Test
    @DisplayName("a non-positive offer price is rejected before it can reach the policy layer")
    void rejectsNonPositiveOfferPrice() {
        when(responseSpec.entity(eq(LlmDiscountProposal.class)))
                .thenReturn(new LlmDiscountProposal(new BigDecimal("100.00"), 0L, "Give it away."));

        assertThatThrownBy(this::propose)
                .isInstanceOf(AiAnalysisFailedException.class)
                .hasMessageContaining("non-positive offer price");
    }
}
