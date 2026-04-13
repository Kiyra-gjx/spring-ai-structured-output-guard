package io.github.kiyragjx.saiguard.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructuredOutputExecutorTest {

    @Test
    void shouldRepairJsonBeforeRetrying() {
        StructuredOutputExecutor executor = new StructuredOutputExecutor();

        String result = executor.execute(StructuredOutputExecution.<String>builder()
            .systemPrompt("Return JSON")
            .userPrompt("hi")
            .responder((systemPrompt, userPrompt) -> """
                ```json
                {"value":"ok",}
                ```
                """)
            .parser(raw -> {
                if (!raw.contains("\"value\":\"ok\"") || raw.contains(",}")) {
                    throw new IllegalArgumentException("json parse error");
                }
                return raw;
            })
            .build());

        assertEquals("{\"value\":\"ok\"}", result);
    }

    @Test
    void shouldRetryWhenTheFailureLooksLikeStructuredOutputError() {
        StructuredOutputExecutor executor = new StructuredOutputExecutor();
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(StructuredOutputExecution.<String>builder()
            .systemPrompt("Return JSON")
            .userPrompt("hi")
            .responder((systemPrompt, userPrompt) -> {
                if (attempts.incrementAndGet() == 1) {
                    return "{bad json";
                }
                return "{\"value\":\"ok\"}";
            })
            .parser(raw -> {
                if (!raw.startsWith("{\"value\"")) {
                    throw new IllegalArgumentException("json parse error");
                }
                return raw;
            })
            .build());

        assertEquals("{\"value\":\"ok\"}", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldWrapWithStructuredOutputExceptionWhenRetryIsExhausted() {
        StructuredOutputExecutor executor = new StructuredOutputExecutor(
            StructuredOutputOptions.builder().maxAttempts(1).build(),
            new StructuredOutputErrorClassifier(),
            new JsonRepairer()
        );

        assertThrows(StructuredOutputException.class, () -> executor.execute(StructuredOutputExecution.<String>builder()
            .systemPrompt("Return JSON")
            .userPrompt("hi")
            .failureMessage("boom")
            .responder((systemPrompt, userPrompt) -> "{bad json")
            .parser(raw -> {
                throw new IllegalArgumentException("json parse error");
            })
            .build()));
    }
}

