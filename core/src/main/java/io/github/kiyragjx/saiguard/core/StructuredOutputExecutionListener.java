package io.github.kiyragjx.saiguard.core;

public interface StructuredOutputExecutionListener {

    default void onRepairAttempted(String logContext) {
    }

    default void onRepairSucceeded(String logContext) {
    }

    default void onRetry(String logContext, int attempt, String errorType) {
    }

    default void onSuccess(String logContext, int attempts, boolean repaired) {
    }

    default void onFailure(String logContext, int attempts, String errorType) {
    }
}
