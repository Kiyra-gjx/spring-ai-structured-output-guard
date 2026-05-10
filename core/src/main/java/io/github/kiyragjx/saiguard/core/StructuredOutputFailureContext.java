package io.github.kiyragjx.saiguard.core;

/**
 * Lightweight metadata attached to a final {@link StructuredOutputException}.
 * <p>
 * The context is intentionally small by default. Optional {@link FailureSnippets} are only populated when explicitly
 * enabled through {@link StructuredOutputOptions}.
 *
 * @param attemptCount completed model attempts before failure; negative values are normalized to {@code 0}
 * @param repairAttempted whether the repair path was entered at least once
 * @param repairSucceeded whether any repair pass produced content that parsed successfully before a later failure
 * @param errorType final classified error type; blank values are normalized to {@link #ERROR_TYPE_UNKNOWN}
 * @param snippets optional content snippets; {@code null} when snippets are disabled or not captured
 */
public record StructuredOutputFailureContext(
    int attemptCount,
    boolean repairAttempted,
    boolean repairSucceeded,
    String errorType,
    FailureSnippets snippets
) {

    /**
     * Error type used when no classifier result is available.
     */
    public static final String ERROR_TYPE_UNKNOWN = "unknown";
    private static final StructuredOutputFailureContext EMPTY = new StructuredOutputFailureContext(
        0,
        false,
        false,
        ERROR_TYPE_UNKNOWN,
        null
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
     * @return empty context with {@code 0} attempts, no repair flags, {@code unknown} error type, and {@code null}
     * snippets
     */
    public static StructuredOutputFailureContext empty() {
        return EMPTY;
    }

    /**
     * Returns whether this context is equal to {@link #empty()}.
     * <p>
     * Snippets are not considered when determining emptiness.
     *
     * @return {@code true} when no failure metadata has been recorded
     */
    public boolean isEmpty() {
        return attemptCount == 0
            && !repairAttempted
            && !repairSucceeded
            && ERROR_TYPE_UNKNOWN.equals(errorType);
    }
}
