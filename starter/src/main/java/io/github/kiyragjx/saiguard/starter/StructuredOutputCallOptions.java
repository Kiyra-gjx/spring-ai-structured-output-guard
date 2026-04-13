package io.github.kiyragjx.saiguard.starter;

public record StructuredOutputCallOptions(
    String logContext,
    String failureMessage
) {

    public static StructuredOutputCallOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String logContext = "";
        private String failureMessage = "";

        public Builder logContext(String logContext) {
            this.logContext = logContext;
            return this;
        }

        public Builder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        public StructuredOutputCallOptions build() {
            return new StructuredOutputCallOptions(logContext, failureMessage);
        }
    }
}

