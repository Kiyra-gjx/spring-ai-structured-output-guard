package io.github.kiyragjx.saiguard.starter;

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

    public StructuredOutputCallOptions(String logContext, String failureMessage) {
        this(logContext, failureMessage, null, null, null, null, null, null, null, null);
    }

    public static StructuredOutputCallOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder logContext(String logContext) {
            this.logContext = logContext;
            return this;
        }

        public Builder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        public Builder maxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder includeLastErrorInRetryPrompt(Boolean includeLastErrorInRetryPrompt) {
            this.includeLastErrorInRetryPrompt = includeLastErrorInRetryPrompt;
            return this;
        }

        public Builder enableRepair(Boolean enableRepair) {
            this.enableRepair = enableRepair;
            return this;
        }

        public Builder maxErrorMessageLength(Integer maxErrorMessageLength) {
            this.maxErrorMessageLength = maxErrorMessageLength;
            return this;
        }

        public Builder strictJsonInstruction(String strictJsonInstruction) {
            this.strictJsonInstruction = strictJsonInstruction;
            return this;
        }

        public Builder retryOnStructuredOutputError(Boolean retryOnStructuredOutputError) {
            this.retryOnStructuredOutputError = retryOnStructuredOutputError;
            return this;
        }

        public Builder retryOnOtherError(Boolean retryOnOtherError) {
            this.retryOnOtherError = retryOnOtherError;
            return this;
        }

        public Builder retryBackoffMillis(Long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
            return this;
        }

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
