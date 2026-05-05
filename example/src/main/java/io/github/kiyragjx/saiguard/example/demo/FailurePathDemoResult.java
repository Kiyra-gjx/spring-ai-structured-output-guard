package io.github.kiyragjx.saiguard.example.demo;

import java.util.List;

public record FailurePathDemoResult(
    List<Scenario> scenarios
) {

    public record Scenario(
        String name,
        String outcome,
        String movie,
        Integer score,
        Integer modelAttempts,
        Failure failure
    ) {
    }

    public record Failure(
        String message,
        int attemptCount,
        boolean repairAttempted,
        boolean repairSucceeded,
        String errorType
    ) {
    }
}
