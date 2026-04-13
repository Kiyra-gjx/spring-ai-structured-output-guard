package io.github.kiyragjx.saiguard.core;

public record StructuredOutputOptions(
    int maxAttempts,
    boolean includeLastErrorInRetryPrompt,
    boolean enableRepair,
    int maxErrorMessageLength,
    String strictJsonInstruction
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

        public StructuredOutputOptions build() {
            return new StructuredOutputOptions(
                maxAttempts,
                includeLastErrorInRetryPrompt,
                enableRepair,
                maxErrorMessageLength,
                strictJsonInstruction
            );
        }
    }
}

