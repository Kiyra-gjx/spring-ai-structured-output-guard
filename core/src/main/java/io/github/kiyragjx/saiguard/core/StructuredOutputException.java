package io.github.kiyragjx.saiguard.core;

/**
 * Runtime exception thrown when structured-output parsing cannot be recovered by repair or retry.
 * <p>
 * The exception keeps failure metadata separate from raw model content. It does not attach raw or repaired payloads by
 * default.
 */
public class StructuredOutputException extends RuntimeException {

    /**
     * Failure metadata for this exception.
     */
    private final StructuredOutputFailureContext failureContext;

    /**
     * Creates an exception with an empty failure context.
     *
     * @param message failure message; may be {@code null}
     * @param cause underlying parse, responder, repair, or retry error; may be {@code null}
     */
    public StructuredOutputException(String message, Throwable cause) {
        this(message, cause, StructuredOutputFailureContext.empty());
    }

    /**
     * Creates an exception with explicit failure context.
     *
     * @param message failure message; may be {@code null}
     * @param cause underlying parse, responder, repair, or retry error; may be {@code null}
     * @param failureContext failure metadata; {@code null} is treated as {@link StructuredOutputFailureContext#empty()}
     */
    public StructuredOutputException(
        String message,
        Throwable cause,
        StructuredOutputFailureContext failureContext
    ) {
        super(message, cause);
        this.failureContext = failureContext == null ? StructuredOutputFailureContext.empty() : failureContext;
    }

    /**
     * Returns the final failure context.
     *
     * @return failure context, never {@code null}
     */
    public StructuredOutputFailureContext failureContext() {
        return failureContext;
    }

    /**
     * Returns the number of completed attempts recorded for the failure.
     *
     * @return completed attempt count, or {@code 0} when context is empty
     */
    public int attemptCount() {
        return failureContext.attemptCount();
    }

    /**
     * Returns whether local repair was attempted at least once.
     *
     * @return {@code true} if the repair path was entered
     */
    public boolean repairAttempted() {
        return failureContext.repairAttempted();
    }

    /**
     * Returns whether repaired content parsed successfully before the overall call later failed.
     *
     * @return {@code true} if any repair pass produced parseable content
     */
    public boolean repairSucceeded() {
        return failureContext.repairSucceeded();
    }

    /**
     * Returns the final error type.
     *
     * @return {@code structured_output}, {@code other}, or {@link StructuredOutputFailureContext#ERROR_TYPE_UNKNOWN}
     */
    public String errorType() {
        return failureContext.errorType();
    }

    StructuredOutputException withFailureContext(StructuredOutputFailureContext failureContext) {
        if (!this.failureContext.isEmpty() || failureContext == null || failureContext.equals(this.failureContext)) {
            return this;
        }
        StructuredOutputException enriched = new StructuredOutputException(getMessage(), getCause(), failureContext);
        enriched.setStackTrace(getStackTrace());
        for (Throwable suppressed : getSuppressed()) {
            enriched.addSuppressed(suppressed);
        }
        return enriched;
    }
}
