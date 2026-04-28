package io.github.kiyragjx.saiguard.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class CompositeStructuredOutputExecutionListener implements StructuredOutputExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(CompositeStructuredOutputExecutionListener.class);

    private final List<StructuredOutputExecutionListener> listeners;

    public CompositeStructuredOutputExecutionListener(List<StructuredOutputExecutionListener> listeners) {
        Objects.requireNonNull(listeners, "listeners cannot be null");
        this.listeners = List.copyOf(listeners);
    }

    @Override
    public void onRepairAttempted(String logContext) {
        notifyListeners(listener -> listener.onRepairAttempted(logContext), "onRepairAttempted");
    }

    @Override
    public void onRepairSucceeded(String logContext) {
        notifyListeners(listener -> listener.onRepairSucceeded(logContext), "onRepairSucceeded");
    }

    @Override
    public void onRetry(String logContext, int attempt, String errorType) {
        notifyListeners(listener -> listener.onRetry(logContext, attempt, errorType), "onRetry");
    }

    @Override
    public void onSuccess(String logContext, int attempts, boolean repaired) {
        notifyListeners(listener -> listener.onSuccess(logContext, attempts, repaired), "onSuccess");
    }

    @Override
    public void onFailure(String logContext, int attempts, String errorType) {
        notifyListeners(listener -> listener.onFailure(logContext, attempts, errorType), "onFailure");
    }

    private void notifyListeners(Consumer<StructuredOutputExecutionListener> notification, String callbackName) {
        for (StructuredOutputExecutionListener listener : listeners) {
            try {
                notification.accept(listener);
            } catch (RuntimeException e) {
                log.warn("Structured output execution listener {} failed during {}", listener.getClass().getName(), callbackName, e);
            }
        }
    }
}
