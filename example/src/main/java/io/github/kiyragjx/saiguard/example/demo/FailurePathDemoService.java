package io.github.kiyragjx.saiguard.example.demo;

import io.github.kiyragjx.saiguard.core.StructuredOutputException;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecution;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutor;
import io.github.kiyragjx.saiguard.core.StructuredOutputOptions;
import io.github.kiyragjx.saiguard.example.demo.FailurePathDemoResult.Failure;
import io.github.kiyragjx.saiguard.example.demo.FailurePathDemoResult.Scenario;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FailurePathDemoService {

    private final StructuredOutputExecutor executor;
    private final BeanOutputConverter<MovieReview> outputConverter = new BeanOutputConverter<>(MovieReview.class);

    public FailurePathDemoService(StructuredOutputExecutor executor) {
        this.executor = executor;
    }

    public FailurePathDemoResult runAll() {
        return new FailurePathDemoResult(List.of(
            codeFenceRepair(),
            trailingCommaRepair(),
            repairFailureThenRetry(),
            finalFailureContext(),
            perCallOverride()
        ));
    }

    private Scenario codeFenceRepair() {
        AtomicInteger attempts = new AtomicInteger();
        MovieReview review = execute(
            "markdown-code-fence",
            attempts,
            defaultOptions(),
            """
                ```json
                {"movie":"Interstellar","score":97,"strengths":["Visuals"],"weaknesses":["Exposition"],"summary":"A bold space epic"}
                ```
                """
        );
        return success("markdown-code-fence", "repaired", review, attempts);
    }

    private Scenario trailingCommaRepair() {
        AtomicInteger attempts = new AtomicInteger();
        MovieReview review = execute(
            "trailing-comma",
            attempts,
            defaultOptions(),
            """
                {"movie":"Arrival","score":95,"strengths":["Language","Mood",],"weaknesses":["Pacing",],"summary":"Quiet and precise",}
                """
        );
        return success("trailing-comma", "repaired", review, attempts);
    }

    private Scenario repairFailureThenRetry() {
        AtomicInteger attempts = new AtomicInteger();
        MovieReview review = execute(
            "repair-failure-then-retry",
            attempts,
            defaultOptions(),
            "{\"movie\":\"Dune\",\"score\":94",
            """
                {"movie":"Dune","score":94,"strengths":["Scale"],"weaknesses":["Abrupt ending"],"summary":"A huge desert epic"}
                """
        );
        return success("repair-failure-then-retry", "retried", review, attempts);
    }

    private Scenario finalFailureContext() {
        AtomicInteger attempts = new AtomicInteger();
        try {
            execute(
                "final-failure-context",
                attempts,
                StructuredOutputOptions.builder()
                    .maxAttempts(2)
                    .build(),
                "{\"movie\":\"Bad JSON\"",
                "{\"movie\":\"Still bad\""
            );
            throw new IllegalStateException("final-failure-context demo unexpectedly succeeded");
        } catch (StructuredOutputException e) {
            return new Scenario(
                "final-failure-context",
                "failed",
                null,
                null,
                attempts.get(),
                new Failure(
                    e.getMessage(),
                    e.attemptCount(),
                    e.repairAttempted(),
                    e.repairSucceeded(),
                    e.errorType()
                )
            );
        }
    }

    private Scenario perCallOverride() {
        AtomicInteger attempts = new AtomicInteger();
        try {
            execute(
                "per-call-disable-repair",
                attempts,
                StructuredOutputOptions.builder()
                    .maxAttempts(1)
                    .enableRepair(false)
                    .build(),
                """
                    ```json
                    {"movie":"Nope","score":88,"strengths":["Tension"],"weaknesses":["Opacity"],"summary":"Strange and sharp"}
                    ```
                    """
            );
            throw new IllegalStateException("per-call-disable-repair demo unexpectedly succeeded");
        } catch (StructuredOutputException e) {
            return new Scenario(
                "per-call-disable-repair",
                "failed-fast",
                null,
                null,
                attempts.get(),
                new Failure(
                    e.getMessage(),
                    e.attemptCount(),
                    e.repairAttempted(),
                    e.repairSucceeded(),
                    e.errorType()
                )
            );
        }
    }

    private MovieReview execute(
        String scenario,
        AtomicInteger attempts,
        StructuredOutputOptions options,
        String... scriptedResponses
    ) {
        return executor.execute(StructuredOutputExecution.<MovieReview>builder()
            .systemPrompt("Return a movie review JSON object.\n" + outputConverter.getFormat())
            .userPrompt("Run demo scenario: " + scenario)
            .logContext("example-" + scenario)
            .failureMessage("Example scenario failed: " + scenario)
            .responder((systemPrompt, userPrompt) -> {
                int attempt = attempts.incrementAndGet();
                int index = Math.min(attempt - 1, scriptedResponses.length - 1);
                return scriptedResponses[index];
            })
            .parser(this::parseStrictJsonObject)
            .build(), options);
    }

    private MovieReview parseStrictJsonObject(String rawContent) {
        if (rawContent == null || !rawContent.trim().startsWith("{")) {
            throw new IllegalArgumentException("json parse error: response must start with a JSON object");
        }
        return outputConverter.convert(rawContent);
    }

    private StructuredOutputOptions defaultOptions() {
        return executor.defaultOptions();
    }

    private Scenario success(String name, String outcome, MovieReview review, AtomicInteger attempts) {
        return new Scenario(
            name,
            outcome,
            review.movie(),
            review.score(),
            attempts.get(),
            null
        );
    }
}
