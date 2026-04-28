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

    public <T> T execute(StructuredOutputExecution<T> execution) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= options.maxAttempts(); attempt++) {
            String attemptSystemPrompt = attempt == 1
                ? execution.systemPrompt()
                : buildRetrySystemPrompt(execution.systemPrompt(), lastError);

            try {
                String rawContent = execution.responder().respond(attemptSystemPrompt, execution.userPrompt());
                ParseResult<T> parseResult = parseWithRepair(execution.parser(), rawContent, execution.logContext());
                executionListener.onSuccess(safeLogContext(execution.logContext()), attempt, parseResult.repaired());
                return parseResult.value();
            } catch (StructuredOutputException e) {
                executionListener.onFailure(safeLogContext(execution.logContext()), attempt, errorType(e));
                throw e;
            } catch (Exception e) {
                if (!shouldRetry(e, attempt)) {
                    executionListener.onFailure(safeLogContext(execution.logContext()), attempt, errorType(e));
                    throw new StructuredOutputException(buildFailureMessage(execution), e);
                }
                lastError = e;
                executionListener.onRetry(safeLogContext(execution.logContext()), attempt + 1, errorType(e));
                log.warn("{} structured output parsing failed, retrying. attempt={}, error={}",
                    safeLogContext(execution.logContext()), attempt, sanitizeErrorMessage(e.getMessage()));
            }
        }

        executionListener.onFailure(safeLogContext(execution.logContext()), options.maxAttempts(), errorType(lastError));
        throw new StructuredOutputException(buildFailureMessage(execution), lastError);
    }

    private boolean shouldRetry(Exception error, int attempt) {
        return attempt < options.maxAttempts() && errorClassifier.isStructuredOutputError(error);
    }

    private String buildRetrySystemPrompt(String systemPrompt, Exception lastError) {
        StringBuilder prompt = new StringBuilder(systemPrompt)
            .append("\n\n")
            .append(options.strictJsonInstruction())
            .append("\nThe previous response could not be parsed as valid JSON. Return only valid JSON.");

        if (options.includeLastErrorInRetryPrompt() && lastError != null && lastError.getMessage() != null) {
            prompt.append("\nPrevious parse error: ").append(sanitizeErrorMessage(lastError.getMessage()));
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

    private String sanitizeErrorMessage(String message) {
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

    private <T> ParseResult<T> parseWithRepair(StructuredOutputParser<T> parser, String rawContent, String logContext) throws Exception {
        try {
            return new ParseResult<>(parser.parse(rawContent), false);
        } catch (Exception originalError) {
            if (!options.enableRepair()) {
                throw originalError;
            }

            executionListener.onRepairAttempted(safeLogContext(logContext));
            String repaired = jsonRepairer.repair(rawContent);
            if (repaired == null || repaired.equals(rawContent)) {
                throw originalError;
            }

            try {
                T value = parser.parse(repaired);
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
}
