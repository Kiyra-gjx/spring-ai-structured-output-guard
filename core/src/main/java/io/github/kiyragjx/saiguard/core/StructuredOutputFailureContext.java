package io.github.kiyragjx.saiguard.core;

public record StructuredOutputFailureContext(
    int attemptCount,
    boolean repairAttempted,
    boolean repairSucceeded,
    String errorType
) {

    public static final String ERROR_TYPE_UNKNOWN = "unknown";

    public StructuredOutputFailureContext {
        attemptCount = Math.max(0, attemptCount);
        errorType = (errorType == null || errorType.isBlank()) ? ERROR_TYPE_UNKNOWN : errorType;
    }

    public static StructuredOutputFailureContext empty() {
        return new StructuredOutputFailureContext(0, false, false, ERROR_TYPE_UNKNOWN);
    }
}
