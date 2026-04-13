package io.github.kiyragjx.saiguard.core;

public record StructuredOutputExecution<T>(
    String systemPrompt,
    String userPrompt,
    String logContext,
    String failureMessage,
    StructuredOutputResponder responder,
    StructuredOutputParser<T> parser
) {

    public StructuredOutputExecution {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt must not be blank");
        }
        if (userPrompt == null) {
            throw new IllegalArgumentException("userPrompt must not be null");
        }
        if (responder == null) {
            throw new IllegalArgumentException("responder must not be null");
        }
        if (parser == null) {
            throw new IllegalArgumentException("parser must not be null");
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private String systemPrompt;
        private String userPrompt = "";
        private String logContext = "";
        private String failureMessage = "";
        private StructuredOutputResponder responder;
        private StructuredOutputParser<T> parser;

        public Builder<T> systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder<T> userPrompt(String userPrompt) {
            this.userPrompt = userPrompt;
            return this;
        }

        public Builder<T> logContext(String logContext) {
            this.logContext = logContext;
            return this;
        }

        public Builder<T> failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        public Builder<T> responder(StructuredOutputResponder responder) {
            this.responder = responder;
            return this;
        }

        public Builder<T> parser(StructuredOutputParser<T> parser) {
            this.parser = parser;
            return this;
        }

        public StructuredOutputExecution<T> build() {
            return new StructuredOutputExecution<>(
                systemPrompt,
                userPrompt,
                logContext,
                failureMessage,
                responder,
                parser
            );
        }
    }
}

