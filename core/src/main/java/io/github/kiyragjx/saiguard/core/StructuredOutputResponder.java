package io.github.kiyragjx.saiguard.core;

/**
 * Produces model content for one attempt.
 */
@FunctionalInterface
public interface StructuredOutputResponder {

    /**
     * Calls the model or other content source.
     *
     * @param systemPrompt effective system prompt for this attempt; retry attempts include stricter JSON instructions
     * @param userPrompt user prompt from the execution
     * @return raw model content to parse; returning {@code null} delegates handling to the parser
     * @throws Exception when the call itself fails; retry depends on the configured error policy
     */
    String respond(String systemPrompt, String userPrompt) throws Exception;
}
