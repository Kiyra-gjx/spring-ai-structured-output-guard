package io.github.kiyragjx.saiguard.core;

/**
 * Parses raw or repaired model content into a structured Java value.
 *
 * @param <T> parsed result type
 */
@FunctionalInterface
public interface StructuredOutputParser<T> {

    /**
     * Parses model content.
     *
     * @param rawContent raw or repaired content; may be {@code null} if the responder returned {@code null}
     * @return parsed value
     * @throws Exception when parsing fails; structured-output-looking failures may be repaired or retried by the
     * executor
     */
    T parse(String rawContent) throws Exception;
}
