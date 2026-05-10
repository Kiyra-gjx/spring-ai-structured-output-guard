package io.github.kiyragjx.saiguard.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a structured-output call with parsing, optional local JSON repair, retry, and failure context enrichment.
 * <p>
 * This is the main core API when you integrate the library without the Spring Boot starter. Callers provide a
 * {@link StructuredOutputExecution} containing the model call and parser, and the executor applies
 * {@link StructuredOutputOptions} consistently around that workflow.
 */
public class StructuredOutputExecutor {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputExecutor.class);
    private static final StructuredOutputExecutionListener NO_OP_LISTENER = new StructuredOutputExecutionListener() {
    };

    private final StructuredOutputOptions options;
    private final StructuredOutputErrorClassifier errorClassifier;
    private final JsonRepairer jsonRepairer;
    private final StructuredOutputExecutionListener executionListener;
    private final RetrySleeper retrySleeper;

    /**
     * Creates an executor with default options, the default structured-output error classifier, the default JSON
     * repairer, and no execution listener.
     */
    public StructuredOutputExecutor() {
        this(StructuredOutputOptions.defaults(), new StructuredOutputErrorClassifier(), new JsonRepairer(), NO_OP_LISTENER);
    }

    /**
     * Creates an executor with custom defaults and extension points.
     *
     * @param options default execution options; if later per-call options are omitted, these values are used
     * @param errorClassifier classifier used to decide whether an exception is retryable as a structured-output error
     * @param jsonRepairer repairer used when parsing fails and repair is enabled
     */
    public StructuredOutputExecutor(
        StructuredOutputOptions options,
        StructuredOutputErrorClassifier errorClassifier,
        JsonRepairer jsonRepairer
    ) {
        this(options, errorClassifier, jsonRepairer, NO_OP_LISTENER);
    }

    /**
     * Creates an executor with custom defaults, extension points, and lifecycle observation.
     *
     * @param options default execution options; if later per-call options are omitted, these values are used
     * @param errorClassifier classifier used to decide whether an exception is retryable as a structured-output error
     * @param jsonRepairer repairer used when parsing fails and repair is enabled
     * @param executionListener optional listener for repair, retry, success, and failure events; {@code null} disables
     * listener callbacks
     */
    public StructuredOutputExecutor(
        StructuredOutputOptions options,
        StructuredOutputErrorClassifier errorClassifier,
        JsonRepairer jsonRepairer,
        StructuredOutputExecutionListener executionListener
    ) {
        this(options, errorClassifier, jsonRepairer, executionListener, Thread::sleep);
    }

    StructuredOutputExecutor(
        StructuredOutputOptions options,
        StructuredOutputErrorClassifier errorClassifier,
        JsonRepairer jsonRepairer,
        StructuredOutputExecutionListener executionListener,
        RetrySleeper retrySleeper
    ) {
        this.options = options;
        this.errorClassifier = errorClassifier;
        this.jsonRepairer = jsonRepairer;
        this.executionListener = executionListener == null ? NO_OP_LISTENER : executionListener;
        this.retrySleeper = retrySleeper == null ? Thread::sleep : retrySleeper;
    }

    /**
     * Returns the options configured on this executor.
     * <p>
     * Starter per-call overrides are merged against this value, and direct core users can inspect it before deciding
     * whether to pass call-specific options to {@link #execute(StructuredOutputExecution, StructuredOutputOptions)}.
     *
     * @return the executor-level default options
     */
    public StructuredOutputOptions defaultOptions() {
        return options;
    }

    /**
     * Executes a structured-output workflow using the executor default options.
     *
     * @param execution model call and parser definition; must not be {@code null}
     * @param <T> parsed result type
     * @return parsed result from the first successful raw or repaired response
     * @throws StructuredOutputException when all configured attempts fail
     */
    public <T> T execute(StructuredOutputExecution<T> execution) {
        return execute(execution, options);
    }

    /**
     * Executes a structured-output workflow with optional call-specific options.
     * <p>
     * Each attempt calls the execution responder, parses the raw content, optionally tries local JSON repair after a
     * parse failure, and then retries only when the configured retry policy allows it. Final failures are wrapped in
     * {@link StructuredOutputException} with {@link StructuredOutputFailureContext}.
     *
     * @param execution model call and parser definition; must not be {@code null}
     * @param callOptions options for this call; {@code null} falls back to {@link #defaultOptions()}
     * @param <T> parsed result type
     * @return parsed result from the first successful raw or repaired response
     * @throws StructuredOutputException when all configured attempts fail, repair fails to recover, or retry backoff is
     * interrupted
     */
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
                throw e.withFailureContext(failureTracker.toContext(
                    effectiveOptions.failureSnippetsEnabled(), effectiveOptions.failureSnippetsMaxLength()));
            } catch (Exception e) {
                failureTracker.recordAttempt(attempt, errorType(e));
                if (!shouldRetry(effectiveOptions, e, attempt)) {
                    executionListener.onFailure(safeLogContext(execution.logContext()), attempt, errorType(e));
                    throw new StructuredOutputException(buildFailureMessage(execution), e, failureTracker.toContext(
                        effectiveOptions.failureSnippetsEnabled(), effectiveOptions.failureSnippetsMaxLength()));
                }
                lastError = e;
                try {
                    sleepBeforeRetry(effectiveOptions);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    String interruptedErrorType = errorType(interrupted);
                    failureTracker.recordAttempt(attempt, interruptedErrorType);
                    executionListener.onFailure(safeLogContext(execution.logContext()), attempt, interruptedErrorType);
                    StructuredOutputException interruptedException = new StructuredOutputException(
                        "Interrupted while waiting to retry structured output parsing",
                        interrupted,
                        failureTracker.toContext(
                            effectiveOptions.failureSnippetsEnabled(), effectiveOptions.failureSnippetsMaxLength())
                    );
                    interruptedException.addSuppressed(e);
                    throw interruptedException;
                }
                executionListener.onRetry(safeLogContext(execution.logContext()), attempt + 1, errorType(e));
                log.warn("{} structured output parsing failed, retrying. attempt={}, error={}",
                    safeLogContext(execution.logContext()), attempt, sanitizeErrorMessage(effectiveOptions, e.getMessage()));
            }
        }

        String errorType = errorType(lastError);
        failureTracker.recordAttempt(effectiveOptions.maxAttempts(), errorType);
        executionListener.onFailure(safeLogContext(execution.logContext()), effectiveOptions.maxAttempts(), errorType);
        throw new StructuredOutputException(buildFailureMessage(execution), lastError, failureTracker.toContext(
            effectiveOptions.failureSnippetsEnabled(), effectiveOptions.failureSnippetsMaxLength()));
    }

    private boolean shouldRetry(StructuredOutputOptions options, Exception error, int attempt) {
        if (attempt >= options.maxAttempts()) {
            return false;
        }
        if (errorClassifier.isStructuredOutputError(error)) {
            return options.retryOnStructuredOutputError();
        }
        return options.retryOnOtherError();
    }

    private void sleepBeforeRetry(StructuredOutputOptions options) throws InterruptedException {
        if (options.retryBackoffMillis() == 0) {
            return;
        }
        retrySleeper.sleep(options.retryBackoffMillis());
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
        failureTracker.recordRawContent(rawContent);
        try {
            return new ParseResult<>(parser.parse(rawContent), false);
        } catch (Exception originalError) {
            if (!options.enableRepair()) {
                throw originalError;
            }

            failureTracker.recordRepairAttempted();
            executionListener.onRepairAttempted(safeLogContext(logContext));
            String safeLogContext = safeLogContext(logContext);
            String repaired = jsonRepairer.repair(rawContent, (stepName, error) -> {
                executionListener.onRepairStepFailed(safeLogContext, stepName, error);
                log.warn("{} JSON repair step failed. step={}, error={}",
                    safeLogContext, stepName, sanitizeErrorMessage(options, error.getMessage()));
            });
            failureTracker.recordRepairedContent(repaired);
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

    @FunctionalInterface
    interface RetrySleeper {

        void sleep(long millis) throws InterruptedException;
    }

    private static final class ExecutionFailureTracker {
        private int attemptCount;
        private boolean repairAttempted;
        private boolean repairSucceeded;
        private String errorType = StructuredOutputFailureContext.ERROR_TYPE_UNKNOWN;
        private String lastRawContent;
        private String lastRepairedContent;

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

        private void recordRawContent(String rawContent) {
            this.lastRawContent = rawContent;
        }

        private void recordRepairedContent(String repairedContent) {
            this.lastRepairedContent = repairedContent;
        }

        private StructuredOutputFailureContext toContext(boolean snippetsEnabled, int snippetsMaxLength) {
            return new StructuredOutputFailureContext(
                attemptCount,
                repairAttempted,
                repairSucceeded,
                errorType,
                buildSnippets(snippetsEnabled, snippetsMaxLength)
            );
        }

        private FailureSnippets buildSnippets(boolean enabled, int maxLength) {
            if (!enabled) {
                return null;
            }
            return new FailureSnippets(
                truncate(lastRawContent, maxLength),
                truncate(lastRepairedContent, maxLength)
            );
        }

        private static String truncate(String content, int maxLength) {
            if (content == null) {
                return null;
            }
            return content.length() > maxLength ? content.substring(0, maxLength) : content;
        }
    }
}
