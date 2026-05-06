package io.github.kiyragjx.saiguard.core;

public record StructuredOutputOptions(
    int maxAttempts,
    boolean includeLastErrorInRetryPrompt,
    boolean enableRepair,
    int maxErrorMessageLength,
    String strictJsonInstruction,
    boolean retryOnStructuredOutputError,
    boolean retryOnOtherError,
    long retryBackoffMillis
) {

    private static final String DEFAULT_STRICT_JSON_INSTRUCTION = """
        Return only valid JSON.
        Rules:
        1. Do not wrap the response in Markdown code fences.
        2. Do not add explanations, prefixes, or suffixes.
        3. Escape quotes correctly inside string values.
        4. Do not include literal newlines inside JSON string values. Use \\n instead.
        """;

    public StructuredOutputOptions {
        maxAttempts = Math.max(1, maxAttempts);
        maxErrorMessageLength = Math.max(32, maxErrorMessageLength);
        strictJsonInstruction = (strictJsonInstruction == null || strictJsonInstruction.isBlank())
            ? DEFAULT_STRICT_JSON_INSTRUCTION
            : strictJsonInstruction.trim();
        retryBackoffMillis = Math.max(0, retryBackoffMillis);
    }

    public static StructuredOutputOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxAttempts = 2;
        private boolean includeLastErrorInRetryPrompt = true;
        private boolean enableRepair = true;
        private int maxErrorMessageLength = 200;
        private String strictJsonInstruction = DEFAULT_STRICT_JSON_INSTRUCTION;
        private boolean retryOnStructuredOutputError = true;
        private boolean retryOnOtherError = false;
        private long retryBackoffMillis = 0;

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder includeLastErrorInRetryPrompt(boolean includeLastErrorInRetryPrompt) {
            this.includeLastErrorInRetryPrompt = includeLastErrorInRetryPrompt;
            return this;
        }

        public Builder enableRepair(boolean enableRepair) {
            this.enableRepair = enableRepair;
            return this;
        }

        public Builder maxErrorMessageLength(int maxErrorMessageLength) {
            this.maxErrorMessageLength = maxErrorMessageLength;
            return this;
        }

        public Builder strictJsonInstruction(String strictJsonInstruction) {
            this.strictJsonInstruction = strictJsonInstruction;
            return this;
        }

        public Builder retryOnStructuredOutputError(boolean retryOnStructuredOutputError) {
            this.retryOnStructuredOutputError = retryOnStructuredOutputError;
            return this;
        }

        public Builder retryOnOtherError(boolean retryOnOtherError) {
            this.retryOnOtherError = retryOnOtherError;
            return this;
        }

        public Builder retryBackoffMillis(long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
            return this;
        }

        public StructuredOutputOptions build() {
            return new StructuredOutputOptions(
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
