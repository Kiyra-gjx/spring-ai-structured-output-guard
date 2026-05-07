package io.github.kiyragjx.saiguard.core;

/**
 * Lightweight metadata attached to a final {@link StructuredOutputException}.
 * <p>
 * The context is intentionally small and does not include raw model output or repaired content.
 *
 * @param attemptCount completed model attempts before failure; negative values are normalized to {@code 0}
 * @param repairAttempted whether the repair path was entered at least once
 * @param repairSucceeded whether any repair pass produced content that parsed successfully before a later failure
 * @param errorType final classified error type; blank values are normalized to {@link #ERROR_TYPE_UNKNOWN}
 */
public record StructuredOutputFailureContext(
    int attemptCount,
    boolean repairAttempted,
    boolean repairSucceeded,
    String errorType
) {

    /**
     * Error type used when no classifier result is available.
     */
    public static final String ERROR_TYPE_UNKNOWN = "unknown";
    private static final StructuredOutputFailureContext EMPTY = new StructuredOutputFailureContext(
        0,
        false,
        false,
        ERROR_TYPE_UNKNOWN
    );

    /**
     * Normalizes negative attempt counts and blank error types.
     */
    public StructuredOutputFailureContext {
        attemptCount = Math.max(0, attemptCount);
        errorType = (errorType == null || errorType.isBlank()) ? ERROR_TYPE_UNKNOWN : errorType;
    }

    /**
     * Returns the shared empty failure context.
     *
     * @return empty context with {@code 0} attempts, no repair flags, and {@code unknown} error type
     */
    public static StructuredOutputFailureContext empty() {
        return EMPTY;
    }

    /**
     * Returns whether this context is equal to {@link #empty()}.
     *
     * @return {@code true} when no failure metadata has been recorded
     */
    public boolean isEmpty() {
        return equals(EMPTY);
    }
}
