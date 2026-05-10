package io.github.kiyragjx.saiguard.core;

/**
 * Immutable retry, repair, prompt, and failure-message options for {@link StructuredOutputExecutor}.
 *
 * @param maxAttempts total model attempts including the first call; values below {@code 1} are normalized to {@code 1}
 * @param includeLastErrorInRetryPrompt whether a sanitized parse error is included in retry instructions
 * @param enableRepair whether local JSON repair is attempted after a parser failure before deciding to retry
 * @param maxErrorMessageLength maximum sanitized parse-error length in retry prompts; values below {@code 32} are
 * normalized to {@code 32}
 * @param strictJsonInstruction retry instruction appended after the first failed attempt; {@code null} or blank uses
 * the built-in instruction
 * @param retryOnStructuredOutputError whether classifier-positive structured-output errors are retryable while attempts
 * remain
 * @param retryOnOtherError whether classifier-negative errors are retryable while attempts remain; defaults to
 * {@code false}
 * @param retryBackoffMillis fixed wait before each retry; negative values are normalized to {@code 0}
 * @param failureSnippetsEnabled whether failure content snippets are captured and attached to the final exception
 * @param failureSnippetsMaxLength maximum length for failure content snippets; values below {@code 64} are normalized
 * to {@code 64}
 */
public record StructuredOutputOptions(
    int maxAttempts,
    boolean includeLastErrorInRetryPrompt,
    boolean enableRepair,
    int maxErrorMessageLength,
    String strictJsonInstruction,
    boolean retryOnStructuredOutputError,
    boolean retryOnOtherError,
    long retryBackoffMillis,
    boolean failureSnippetsEnabled,
    int failureSnippetsMaxLength
) {

    private static final String DEFAULT_STRICT_JSON_INSTRUCTION = """
        Return only valid JSON.
        Rules:
        1. Do not wrap the response in Markdown code fences.
        2. Do not add explanations, prefixes, or suffixes.
        3. Escape quotes correctly inside string values.
        4. Do not include literal newlines inside JSON string values. Use \\n instead.
        """;

    /**
     * Normalizes option values to documented lower bounds and defaults.
     */
    public StructuredOutputOptions {
        maxAttempts = Math.max(1, maxAttempts);
        maxErrorMessageLength = Math.max(32, maxErrorMessageLength);
        strictJsonInstruction = (strictJsonInstruction == null || strictJsonInstruction.isBlank())
            ? DEFAULT_STRICT_JSON_INSTRUCTION
            : strictJsonInstruction.trim();
        retryBackoffMillis = Math.max(0, retryBackoffMillis);
        failureSnippetsMaxLength = Math.max(64, failureSnippetsMaxLength);
    }

    /**
     * Returns default options matching the starter's out-of-the-box behavior.
     *
     * @return default options
     */
    public static StructuredOutputOptions defaults() {
        return builder().build();
    }

    /**
     * Creates a builder initialized with default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link StructuredOutputOptions}.
     */
    public static final class Builder {
        private int maxAttempts = 2;
        private boolean includeLastErrorInRetryPrompt = true;
        private boolean enableRepair = true;
        private int maxErrorMessageLength = 200;
        private String strictJsonInstruction = DEFAULT_STRICT_JSON_INSTRUCTION;
        private boolean retryOnStructuredOutputError = true;
        private boolean retryOnOtherError = false;
        private long retryBackoffMillis = 0;
        private boolean failureSnippetsEnabled = false;
        private int failureSnippetsMaxLength = 500;

        /**
         * Creates a builder initialized with default values.
         */
        public Builder() {
        }

        /**
         * Sets total attempts including the first call.
         *
         * @param maxAttempts total attempts; values below {@code 1} are normalized when built
         * @return this builder
         */
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Controls whether the sanitized previous parse error is included in retry instructions.
         *
         * @param includeLastErrorInRetryPrompt {@code true} to include the previous error
         * @return this builder
         */
        public Builder includeLastErrorInRetryPrompt(boolean includeLastErrorInRetryPrompt) {
            this.includeLastErrorInRetryPrompt = includeLastErrorInRetryPrompt;
            return this;
        }

        /**
         * Controls whether local JSON repair is attempted before retrying.
         *
         * @param enableRepair {@code true} to enable the repair path
         * @return this builder
         */
        public Builder enableRepair(boolean enableRepair) {
            this.enableRepair = enableRepair;
            return this;
        }

        /**
         * Sets the maximum sanitized error-message length used in retry prompts.
         *
         * @param maxErrorMessageLength maximum length; values below {@code 32} are normalized when built
         * @return this builder
         */
        public Builder maxErrorMessageLength(int maxErrorMessageLength) {
            this.maxErrorMessageLength = maxErrorMessageLength;
            return this;
        }

        /**
         * Sets the strict JSON instruction appended to retry prompts.
         *
         * @param strictJsonInstruction instruction text; {@code null} or blank uses the built-in instruction
         * @return this builder
         */
        public Builder strictJsonInstruction(String strictJsonInstruction) {
            this.strictJsonInstruction = strictJsonInstruction;
            return this;
        }

        /**
         * Controls retry for errors classified as structured-output parsing failures.
         *
         * @param retryOnStructuredOutputError {@code true} to retry while attempts remain
         * @return this builder
         */
        public Builder retryOnStructuredOutputError(boolean retryOnStructuredOutputError) {
            this.retryOnStructuredOutputError = retryOnStructuredOutputError;
            return this;
        }

        /**
         * Controls retry for errors not classified as structured-output parsing failures.
         *
         * @param retryOnOtherError {@code true} to retry while attempts remain
         * @return this builder
         */
        public Builder retryOnOtherError(boolean retryOnOtherError) {
            this.retryOnOtherError = retryOnOtherError;
            return this;
        }

        /**
         * Sets the fixed wait before each retry.
         *
         * @param retryBackoffMillis wait in milliseconds; negative values are normalized when built
         * @return this builder
         */
        public Builder retryBackoffMillis(long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
            return this;
        }

        /**
         * Controls whether failure content snippets are captured and attached to the final exception.
         *
         * @param failureSnippetsEnabled {@code true} to enable snippet capture
         * @return this builder
         */
        public Builder failureSnippetsEnabled(boolean failureSnippetsEnabled) {
            this.failureSnippetsEnabled = failureSnippetsEnabled;
            return this;
        }

        /**
         * Sets the maximum length for failure content snippets.
         *
         * @param failureSnippetsMaxLength maximum length; values below {@code 64} are normalized when built
         * @return this builder
         */
        public Builder failureSnippetsMaxLength(int failureSnippetsMaxLength) {
            this.failureSnippetsMaxLength = failureSnippetsMaxLength;
            return this;
        }

        /**
         * Builds immutable options and applies documented normalization.
         *
         * @return options instance
         */
        public StructuredOutputOptions build() {
            return new StructuredOutputOptions(
                maxAttempts,
                includeLastErrorInRetryPrompt,
                enableRepair,
                maxErrorMessageLength,
                strictJsonInstruction,
                retryOnStructuredOutputError,
                retryOnOtherError,
                retryBackoffMillis,
                failureSnippetsEnabled,
                failureSnippetsMaxLength
            );
        }
    }
}
