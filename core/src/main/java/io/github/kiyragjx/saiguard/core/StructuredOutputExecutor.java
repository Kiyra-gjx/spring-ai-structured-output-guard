package io.github.kiyragjx.saiguard.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructuredOutputExecutor {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputExecutor.class);
    private static final StructuredOutputExecutionListener NO_OP_LISTENER = new StructuredOutputExecutionListener() {
    };

    private final StructuredOutputOptions options;
    private final StructuredOutputErrorClassifier errorClassifier;
    private final JsonRepairer jsonRepairer;
    private final StructuredOutputExecutionListener executionListener;

    public StructuredOutputExecutor() {
        this(StructuredOutputOptions.defaults(), new StructuredOutputErrorClassifier(), new JsonRepairer(), NO_OP_LISTENER);
    }

    public StructuredOutputExecutor(
        StructuredOutputOptions options,
        StructuredOutputErrorClassifier errorClassifier,
        JsonRepairer jsonRepairer
    ) {
        this(options, errorClassifier, jsonRepairer, NO_OP_LISTENER);
    }

    public StructuredOutputExecutor(
        StructuredOutputOptions options,
        StructuredOutputErrorClassifier errorClassifier,
        JsonRepairer jsonRepairer,
        StructuredOutputExecutionListener executionListener
    ) {
        this.options = options;
        this.errorClassifier = errorClassifier;
        this.jsonRepairer = jsonRepairer;
        this.executionListener = executionListener == null ? NO_OP_LISTENER : executionListener;
    }

    public StructuredOutputOptions defaultOptions() {
        return options;
    }

    public <T> T execute(StructuredOutputExecution<T> execution) {
        return execute(execution, options);
    }

    public <T> T execute(StructuredOutputExecution<T> execution, StructuredOutputOptions callOptions) {
        StructuredOutputOptions effectiveOptions = callOptions == null ? options : callOptions;
        Exception lastError = null;
        ExecutionFailureTracker failureTracker = new ExecutionFailureTracker();

        for (int attempt = 1; attempt <= effectiveOptions.maxAttempts(); attempt++) {
            String attemptSystemPrompt = attempt == 1
                ? execution.systemPrompt()
                : buildRetrySystemPrompt(effectiveOptions, execution.systemPrompt(), lastError);

            try {
                String rawContent = execution.responder().respond(attemptSystemPrompt, execution.userPrompt());
                ParseResult<T> parseResult = parseWithRepair(
                    effectiveOptions,
                    execution.parser(),
                    rawContent,
                    execution.logContext(),
                    failureTracker
                );
                executionListener.onSuccess(safeLogContext(execution.logContext()), attempt, parseResult.repaired());
                return parseResult.value();
            } catch (StructuredOutputException e) {
                String errorType = errorType(e);
                failureTracker.recordAttempt(attempt, errorType);
                executionListener.onFailure(safeLogContext(execution.logContext()), attempt, errorType);
                throw e.withFailureContext(failureTracker.toContext());
            } catch (Exception e) {
                failureTracker.recordAttempt(attempt, errorType(e));
                if (!shouldRetry(effectiveOptions, e, attempt)) {
                    executionListener.onFailure(safeLogContext(execution.logContext()), attempt, errorType(e));
                    throw new StructuredOutputException(buildFailureMessage(execution), e, failureTracker.toContext());
                }
                lastError = e;
                executionListener.onRetry(safeLogContext(execution.logContext()), attempt + 1, errorType(e));
                log.warn("{} structured output parsing failed, retrying. attempt={}, error={}",
                    safeLogContext(execution.logContext()), attempt, sanitizeErrorMessage(effectiveOptions, e.getMessage()));
            }
        }

        String errorType = errorType(lastError);
        failureTracker.recordAttempt(effectiveOptions.maxAttempts(), errorType);
        executionListener.onFailure(safeLogContext(execution.logContext()), effectiveOptions.maxAttempts(), errorType);
        throw new StructuredOutputException(buildFailureMessage(execution), lastError, failureTracker.toContext());
    }

    private boolean shouldRetry(StructuredOutputOptions options, Exception error, int attempt) {
        return attempt < options.maxAttempts() && errorClassifier.isStructuredOutputError(error);
    }

    private String buildRetrySystemPrompt(StructuredOutputOptions options, String systemPrompt, Exception lastError) {
        StringBuilder prompt = new StringBuilder(systemPrompt)
            .append("\n\n")
            .append(options.strictJsonInstruction())
            .append("\nThe previous response could not be parsed as valid JSON. Return only valid JSON.");

        if (options.includeLastErrorInRetryPrompt() && lastError != null && lastError.getMessage() != null) {
            prompt.append("\nPrevious parse error: ").append(sanitizeErrorMessage(options, lastError.getMessage()));
        }
        return prompt.toString();
    }

    private String buildFailureMessage(StructuredOutputExecution<?> execution) {
        if (execution.failureMessage() != null && !execution.failureMessage().isBlank()) {
            return execution.failureMessage();
        }
        if (execution.logContext() != null && !execution.logContext().isBlank()) {
            return execution.logContext() + " structured output parsing failed";
        }
        return "Structured output parsing failed";
    }

    private String sanitizeErrorMessage(StructuredOutputOptions options, String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        String oneLine = message.replace('\n', ' ').replace('\r', ' ').trim();
        if (oneLine.length() > options.maxErrorMessageLength()) {
            return oneLine.substring(0, options.maxErrorMessageLength()) + "...";
        }
        return oneLine;
    }

    private String safeLogContext(String logContext) {
        return (logContext == null || logContext.isBlank()) ? "structured-output" : logContext;
    }

    private String errorType(Throwable error) {
        return errorClassifier.isStructuredOutputError(error) ? "structured_output" : "other";
    }

    private <T> ParseResult<T> parseWithRepair(
        StructuredOutputOptions options,
        StructuredOutputParser<T> parser,
        String rawContent,
        String logContext,
        ExecutionFailureTracker failureTracker
    ) throws Exception {
        try {
            return new ParseResult<>(parser.parse(rawContent), false);
        } catch (Exception originalError) {
            if (!options.enableRepair()) {
                throw originalError;
            }

            failureTracker.recordRepairAttempted();
            executionListener.onRepairAttempted(safeLogContext(logContext));
            String repaired = jsonRepairer.repair(rawContent);
            if (repaired == null || repaired.equals(rawContent)) {
                throw originalError;
            }

            try {
                T value = parser.parse(repaired);
                failureTracker.recordRepairSucceeded();
                executionListener.onRepairSucceeded(safeLogContext(logContext));
                log.info("{} parsed successfully after JSON repair", safeLogContext(logContext));
                return new ParseResult<>(value, true);
            } catch (Exception repairedError) {
                originalError.addSuppressed(repairedError);
                throw originalError;
            }
        }
    }

    private record ParseResult<T>(T value, boolean repaired) {
    }

    private static final class ExecutionFailureTracker {
        private int attemptCount;
        private boolean repairAttempted;
        private boolean repairSucceeded;
        private String errorType = StructuredOutputFailureContext.ERROR_TYPE_UNKNOWN;

        private void recordAttempt(int attemptCount, String errorType) {
            this.attemptCount = Math.max(this.attemptCount, attemptCount);
            this.errorType = errorType;
        }

        private void recordRepairAttempted() {
            repairAttempted = true;
        }

        private void recordRepairSucceeded() {
            repairSucceeded = true;
        }

        private StructuredOutputFailureContext toContext() {
            return new StructuredOutputFailureContext(
                attemptCount,
                repairAttempted,
                repairSucceeded,
                errorType
            );
        }
    }
}
