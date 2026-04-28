package io.github.kiyragjx.saiguard.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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
    void shouldRetryWhenJsonIsTruncatedAndRepairCannotRecoverIt() {
        StructuredOutputExecutor executor = new StructuredOutputExecutor();
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(StructuredOutputExecution.<String>builder()
            .systemPrompt("Return JSON")
            .userPrompt("hi")
            .responder((systemPrompt, userPrompt) -> {
                if (attempts.incrementAndGet() == 1) {
                    return "{\"value\":\"ok\"";
                }
                return "{\"value\":\"ok\"}";
            })
            .parser(raw -> {
                if (!raw.endsWith("}")) {
                    throw new IllegalArgumentException("unexpected end-of-input");
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

    @Test
    void shouldNotifyRepairAndSuccessEventsWhenRepairRecoversParsing() {
        RecordingExecutionListener listener = new RecordingExecutionListener();
        StructuredOutputExecutor executor = new StructuredOutputExecutor(
            StructuredOutputOptions.defaults(),
            new StructuredOutputErrorClassifier(),
            new JsonRepairer(),
            listener
        );

        String result = executor.execute(StructuredOutputExecution.<String>builder()
            .systemPrompt("Return JSON")
            .userPrompt("hi")
            .logContext("movie-review")
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
        assertEquals(List.of("repair-attempted:movie-review", "repair-succeeded:movie-review", "success:movie-review:1:true"),
            listener.events);
    }

    @Test
    void shouldNotifyRetryAndFinalSuccessEvents() {
        RecordingExecutionListener listener = new RecordingExecutionListener();
        StructuredOutputExecutor executor = new StructuredOutputExecutor(
            StructuredOutputOptions.defaults(),
            new StructuredOutputErrorClassifier(),
            new JsonRepairer(),
            listener
        );
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(StructuredOutputExecution.<String>builder()
            .systemPrompt("Return JSON")
            .userPrompt("hi")
            .logContext("resume-task")
            .responder((systemPrompt, userPrompt) -> {
                if (attempts.incrementAndGet() == 1) {
                    return "{\"value\":\"ok\"";
                }
                return "{\"value\":\"ok\"}";
            })
            .parser(raw -> {
                if (!raw.endsWith("}")) {
                    throw new IllegalArgumentException("unexpected end-of-input");
                }
                return raw;
            })
            .build());

        assertEquals("{\"value\":\"ok\"}", result);
        assertEquals(List.of("repair-attempted:resume-task", "retry:resume-task:2:structured_output", "success:resume-task:2:false"),
            listener.events);
    }

    @Test
    void shouldNotifyFinalFailureWithClassifiedErrorType() {
        RecordingExecutionListener listener = new RecordingExecutionListener();
        StructuredOutputExecutor executor = new StructuredOutputExecutor(
            StructuredOutputOptions.builder().maxAttempts(1).build(),
            new StructuredOutputErrorClassifier(),
            new JsonRepairer(),
            listener
        );

        assertThrows(StructuredOutputException.class, () -> executor.execute(StructuredOutputExecution.<String>builder()
            .systemPrompt("Return JSON")
            .userPrompt("hi")
            .logContext("resume-task")
            .responder((systemPrompt, userPrompt) -> "{bad json")
            .parser(raw -> {
                throw new IllegalArgumentException("json parse error");
            })
            .build()));

        assertEquals(List.of("repair-attempted:resume-task", "failure:resume-task:1:structured_output"), listener.events);
    }

    @Test
    void shouldKeepMainFlowWorkingWhenOneExecutionListenerFails() {
        RecordingExecutionListener healthyListener = new RecordingExecutionListener();
        CompositeStructuredOutputExecutionListener listener = new CompositeStructuredOutputExecutionListener(List.of(
            new StructuredOutputExecutionListener() {
                @Override
                public void onRepairAttempted(String logContext) {
                    throw new IllegalStateException("listener boom");
                }
            },
            healthyListener
        ));
        StructuredOutputExecutor executor = new StructuredOutputExecutor(
            StructuredOutputOptions.defaults(),
            new StructuredOutputErrorClassifier(),
            new JsonRepairer(),
            listener
        );

        String result = executor.execute(StructuredOutputExecution.<String>builder()
            .systemPrompt("Return JSON")
            .userPrompt("hi")
            .logContext("movie-review")
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
        assertEquals(List.of("repair-attempted:movie-review", "repair-succeeded:movie-review", "success:movie-review:1:true"),
            healthyListener.events);
    }

    private static final class RecordingExecutionListener implements StructuredOutputExecutionListener {

        private final List<String> events = new ArrayList<>();

        @Override
        public void onRepairAttempted(String logContext) {
            events.add("repair-attempted:" + logContext);
        }

        @Override
        public void onRepairSucceeded(String logContext) {
            events.add("repair-succeeded:" + logContext);
        }

        @Override
        public void onRetry(String logContext, int attempt, String errorType) {
            events.add("retry:" + logContext + ":" + attempt + ":" + errorType);
        }

        @Override
        public void onSuccess(String logContext, int attempts, boolean repaired) {
            events.add("success:" + logContext + ":" + attempts + ":" + repaired);
        }

        @Override
        public void onFailure(String logContext, int attempts, String errorType) {
            events.add("failure:" + logContext + ":" + attempts + ":" + errorType);
        }
    }
}
