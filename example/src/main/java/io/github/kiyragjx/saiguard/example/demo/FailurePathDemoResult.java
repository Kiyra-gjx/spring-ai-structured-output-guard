package io.github.kiyragjx.saiguard.example.demo;

import java.util.List;

/**
 * Response body for the scripted failure-path demo endpoint.
 *
 * @param scenarios scenario results in display order
 */
public record FailurePathDemoResult(
    List<Scenario> scenarios
) {

    /**
     * One scripted scenario result.
     *
     * @param name scenario identifier
     * @param outcome high-level outcome label
     * @param movie parsed movie title when the scenario succeeds
     * @param score parsed score when the scenario succeeds
     * @param modelAttempts number of scripted model responses consumed
     * @param failure failure context when the scenario fails
     */
    public record Scenario(
        String name,
        String outcome,
        String movie,
        Integer score,
        Integer modelAttempts,
        Failure failure
    ) {
    }

    /**
     * Failure details exposed by a failed scripted scenario.
     *
     * @param message exception message
     * @param attemptCount attempt count reported by the guard
     * @param repairAttempted whether repair was attempted
     * @param repairSucceeded whether a repair pass ever parsed successfully
     * @param errorType final error type
     */
    public record Failure(
        String message,
        int attemptCount,
        boolean repairAttempted,
        boolean repairSucceeded,
        String errorType
    ) {
    }
}
