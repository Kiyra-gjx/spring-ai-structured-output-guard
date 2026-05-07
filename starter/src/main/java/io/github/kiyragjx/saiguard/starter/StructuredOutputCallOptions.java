package io.github.kiyragjx.saiguard.starter;

/**
 * Per-call overrides for {@link SpringAiStructuredOutputGuard}.
 * <p>
 * This record is intentionally nullable for most option fields: {@code null} means "inherit the starter global
 * configuration." Use it when a single call needs stricter retry, repair, prompt, or failure-message behavior than the
 * application default.
 *
 * @param logContext optional context used in logs and listener callbacks; blank is allowed
 * @param failureMessage optional final exception message; blank falls back to executor-generated text
 * @param maxAttempts optional total attempts including the first call; {@code null} inherits the global value
 * @param includeLastErrorInRetryPrompt optional retry-prompt error inclusion flag; {@code null} inherits the global
 * value
 * @param enableRepair optional local repair flag; {@code null} inherits the global value
 * @param maxErrorMessageLength optional retry-prompt error-message limit; {@code null} inherits the global value
 * @param strictJsonInstruction optional retry instruction; {@code null} inherits the global value
 * @param retryOnStructuredOutputError optional retry policy for structured-output parsing errors; {@code null} inherits
 * the global value
 * @param retryOnOtherError optional retry policy for non-structured-output errors; {@code null} inherits the global
 * value
 * @param retryBackoffMillis optional fixed wait before each retry; {@code null} inherits the global value
 */
public record StructuredOutputCallOptions(
    String logContext,
    String failureMessage,
    Integer maxAttempts,
    Boolean includeLastErrorInRetryPrompt,
    Boolean enableRepair,
    Integer maxErrorMessageLength,
    String strictJsonInstruction,
    Boolean retryOnStructuredOutputError,
    Boolean retryOnOtherError,
    Long retryBackoffMillis
) {

    /**
     * Creates call options with only logging and failure-message metadata.
     *
     * @param logContext optional context used in logs and listener callbacks
     * @param failureMessage optional final exception message
     */
    public StructuredOutputCallOptions(String logContext, String failureMessage) {
        this(logContext, failureMessage, null, null, null, null, null, null, null, null);
    }

    /**
     * Returns empty per-call options, causing all behavior settings to inherit global configuration.
     *
     * @return default call options
     */
    public static StructuredOutputCallOptions defaults() {
        return builder().build();
    }

    /**
     * Creates a builder initialized with blank metadata and inherited behavior settings.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link StructuredOutputCallOptions}.
     */
    public static final class Builder {
        private String logContext = "";
        private String failureMessage = "";
        private Integer maxAttempts;
        private Boolean includeLastErrorInRetryPrompt;
        private Boolean enableRepair;
        private Integer maxErrorMessageLength;
        private String strictJsonInstruction;
        private Boolean retryOnStructuredOutputError;
        private Boolean retryOnOtherError;
        private Long retryBackoffMillis;

        /**
         * Creates a builder initialized with blank metadata and inherited behavior settings.
         */
        public Builder() {
        }

        /**
         * Sets optional log context for this call.
         *
         * @param logContext context used in logs and listener callbacks
         * @return this builder
         */
        public Builder logContext(String logContext) {
            this.logContext = logContext;
            return this;
        }

        /**
         * Sets the final exception message for this call.
         *
         * @param failureMessage custom message used when all attempts fail
         * @return this builder
         */
        public Builder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        /**
         * Overrides total attempts for this call.
         *
         * @param maxAttempts total attempts including the first call, or {@code null} to inherit
         * @return this builder
         */
        public Builder maxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Overrides whether the previous parse error is included in retry prompts.
         *
         * @param includeLastErrorInRetryPrompt override value, or {@code null} to inherit
         * @return this builder
         */
        public Builder includeLastErrorInRetryPrompt(Boolean includeLastErrorInRetryPrompt) {
            this.includeLastErrorInRetryPrompt = includeLastErrorInRetryPrompt;
            return this;
        }

        /**
         * Overrides whether local JSON repair is attempted.
         *
         * @param enableRepair override value, or {@code null} to inherit
         * @return this builder
         */
        public Builder enableRepair(Boolean enableRepair) {
            this.enableRepair = enableRepair;
            return this;
        }

        /**
         * Overrides the sanitized retry-prompt error-message limit.
         *
         * @param maxErrorMessageLength override value, or {@code null} to inherit
         * @return this builder
         */
        public Builder maxErrorMessageLength(Integer maxErrorMessageLength) {
            this.maxErrorMessageLength = maxErrorMessageLength;
            return this;
        }

        /**
         * Overrides the retry-time strict JSON instruction.
         *
         * @param strictJsonInstruction override instruction, or {@code null} to inherit
         * @return this builder
         */
        public Builder strictJsonInstruction(String strictJsonInstruction) {
            this.strictJsonInstruction = strictJsonInstruction;
            return this;
        }

        /**
         * Overrides retry behavior for structured-output parsing errors.
         *
         * @param retryOnStructuredOutputError override value, or {@code null} to inherit
         * @return this builder
         */
        public Builder retryOnStructuredOutputError(Boolean retryOnStructuredOutputError) {
            this.retryOnStructuredOutputError = retryOnStructuredOutputError;
            return this;
        }

        /**
         * Overrides retry behavior for non-structured-output errors.
         *
         * @param retryOnOtherError override value, or {@code null} to inherit
         * @return this builder
         */
        public Builder retryOnOtherError(Boolean retryOnOtherError) {
            this.retryOnOtherError = retryOnOtherError;
            return this;
        }

        /**
         * Overrides the fixed wait before each retry.
         *
         * @param retryBackoffMillis override value in milliseconds, or {@code null} to inherit
         * @return this builder
         */
        public Builder retryBackoffMillis(Long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
            return this;
        }

        /**
         * Builds immutable call options.
         *
         * @return call options
         */
        public StructuredOutputCallOptions build() {
            return new StructuredOutputCallOptions(
                logContext,
                failureMessage,
                maxAttempts,
                includeLastErrorInRetryPrompt,
                enableRepair,
                maxErrorMessageLength,
                strictJsonInstruction,
                retryOnStructuredOutputError,
                retryOnOtherError,
                retryBackoffMillis
            );
        }
    }
}
