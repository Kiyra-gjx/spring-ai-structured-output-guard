package io.github.kiyragjx.saiguard.core;

/**
 * Observes structured-output execution lifecycle events.
 * <p>
 * Implementations are extension points for metrics, tracing, logging, or testing. Callbacks should avoid throwing;
 * {@link CompositeStructuredOutputExecutionListener} isolates listener failures, but direct executor usage with a
 * single listener should still keep callbacks lightweight.
 */
public interface StructuredOutputExecutionListener {

    /**
     * Called after parsing fails and local repair is about to run.
     *
     * @param logContext sanitized log context for the execution
     */
    default void onRepairAttempted(String logContext) {
    }

    /**
     * Called when repaired content parses successfully.
     *
     * @param logContext sanitized log context for the execution
     */
    default void onRepairSucceeded(String logContext) {
    }

    /**
     * Called when an individual repair step fails.
     *
     * @param logContext sanitized log context for the execution
     * @param stepName repair step name
     * @param error wrapped step failure
     */
    default void onRepairStepFailed(String logContext, String stepName, Throwable error) {
    }

    /**
     * Called when another model attempt is scheduled.
     *
     * @param logContext sanitized log context for the execution
     * @param attempt next attempt number, starting at {@code 2}
     * @param errorType error type that caused the retry
     */
    default void onRetry(String logContext, int attempt, String errorType) {
    }

    /**
     * Called when the execution returns successfully.
     *
     * @param logContext sanitized log context for the execution
     * @param attempts attempt number that succeeded
     * @param repaired whether the successful result came from repaired content
     */
    default void onSuccess(String logContext, int attempts, boolean repaired) {
    }

    /**
     * Called when the execution reaches a final failure.
     *
     * @param logContext sanitized log context for the execution
     * @param attempts completed attempts at failure time
     * @param errorType final error type
     */
    default void onFailure(String logContext, int attempts, String errorType) {
    }
}
