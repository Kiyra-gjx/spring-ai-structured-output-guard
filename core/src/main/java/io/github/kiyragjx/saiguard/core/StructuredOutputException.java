package io.github.kiyragjx.saiguard.core;

public class StructuredOutputException extends RuntimeException {

    private final StructuredOutputFailureContext failureContext;

    public StructuredOutputException(String message, Throwable cause) {
        this(message, cause, StructuredOutputFailureContext.empty());
    }

    public StructuredOutputException(
        String message,
        Throwable cause,
        StructuredOutputFailureContext failureContext
    ) {
        super(message, cause);
        this.failureContext = failureContext == null ? StructuredOutputFailureContext.empty() : failureContext;
    }

    public StructuredOutputFailureContext failureContext() {
        return failureContext;
    }

    public int attemptCount() {
        return failureContext.attemptCount();
    }

    public boolean repairAttempted() {
        return failureContext.repairAttempted();
    }

    public boolean repairSucceeded() {
        return failureContext.repairSucceeded();
    }

    public String errorType() {
        return failureContext.errorType();
    }
}
