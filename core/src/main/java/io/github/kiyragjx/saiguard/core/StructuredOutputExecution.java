package io.github.kiyragjx.saiguard.core;

/**
 * Describes one structured-output operation for {@link StructuredOutputExecutor}.
 *
 * @param systemPrompt base system prompt; must not be {@code null}, blank, or only whitespace
 * @param userPrompt user prompt passed to the responder; must not be {@code null}, but may be blank
 * @param logContext optional short context used in logs and listener callbacks; blank values are allowed
 * @param failureMessage optional message for the final {@link StructuredOutputException}; blank values fall back to a
 * generated message
 * @param responder model call abstraction; must not be {@code null}
 * @param parser parser that converts raw or repaired content into {@code T}; must not be {@code null}
 * @param <T> parsed result type
 */
public record StructuredOutputExecution<T>(
    String systemPrompt,
    String userPrompt,
    String logContext,
    String failureMessage,
    StructuredOutputResponder responder,
    StructuredOutputParser<T> parser
) {

    /**
     * Validates required execution fields.
     */
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

    /**
     * Creates a builder with blank optional text fields.
     *
     * @param <T> parsed result type
     * @return a new builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link StructuredOutputExecution}.
     *
     * @param <T> parsed result type
     */
    public static final class Builder<T> {
        private String systemPrompt;
        private String userPrompt = "";
        private String logContext = "";
        private String failureMessage = "";
        private StructuredOutputResponder responder;
        private StructuredOutputParser<T> parser;

        /**
         * Creates a builder with blank optional text fields.
         */
        public Builder() {
        }

        /**
         * Sets the required base system prompt.
         *
         * @param systemPrompt prompt text; must not be {@code null}, blank, or only whitespace
         * @return this builder
         */
        public Builder<T> systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * Sets the user prompt.
         *
         * @param userPrompt prompt text; must not be {@code null}, but may be blank
         * @return this builder
         */
        public Builder<T> userPrompt(String userPrompt) {
            this.userPrompt = userPrompt;
            return this;
        }

        /**
         * Sets the optional log context used by logs and listeners.
         *
         * @param logContext short context value; may be {@code null} or blank
         * @return this builder
         */
        public Builder<T> logContext(String logContext) {
            this.logContext = logContext;
            return this;
        }

        /**
         * Sets the optional failure message for final structured-output exceptions.
         *
         * @param failureMessage message to use when all attempts fail; may be {@code null} or blank
         * @return this builder
         */
        public Builder<T> failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        /**
         * Sets the required responder that calls the model or another content source.
         *
         * @param responder responder implementation; must not be {@code null}
         * @return this builder
         */
        public Builder<T> responder(StructuredOutputResponder responder) {
            this.responder = responder;
            return this;
        }

        /**
         * Sets the required parser for raw or repaired content.
         *
         * @param parser parser implementation; must not be {@code null}
         * @return this builder
         */
        public Builder<T> parser(StructuredOutputParser<T> parser) {
            this.parser = parser;
            return this;
        }

        /**
         * Builds an immutable execution definition and validates required fields.
         *
         * @return execution definition
         * @throws IllegalArgumentException if the system prompt is blank or required collaborators are missing
         */
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
