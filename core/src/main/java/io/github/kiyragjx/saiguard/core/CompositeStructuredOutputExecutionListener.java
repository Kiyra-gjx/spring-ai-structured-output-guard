package io.github.kiyragjx.saiguard.core;

import java.util.List;
import java.util.Objects;

public class CompositeStructuredOutputExecutionListener implements StructuredOutputExecutionListener {

    private final List<StructuredOutputExecutionListener> listeners;

    public CompositeStructuredOutputExecutionListener(List<StructuredOutputExecutionListener> listeners) {
        Objects.requireNonNull(listeners, "listeners cannot be null");
        this.listeners = List.copyOf(listeners);
    }

    @Override
    public void onRepairAttempted(String logContext) {
        listeners.forEach(listener -> listener.onRepairAttempted(logContext));
    }

    @Override
    public void onRepairSucceeded(String logContext) {
        listeners.forEach(listener -> listener.onRepairSucceeded(logContext));
    }

    @Override
    public void onRetry(String logContext, int attempt, String errorType) {
        listeners.forEach(listener -> listener.onRetry(logContext, attempt, errorType));
    }

    @Override
    public void onSuccess(String logContext, int attempts, boolean repaired) {
        listeners.forEach(listener -> listener.onSuccess(logContext, attempts, repaired));
    }

    @Override
    public void onFailure(String logContext, int attempts, String errorType) {
        listeners.forEach(listener -> listener.onFailure(logContext, attempts, errorType));
    }
}
