package io.github.kiyragjx.saiguard.core;

/**
 * Optional content snippets attached to a {@link StructuredOutputFailureContext}.
 * <p>
 * Snippets are only populated when explicitly enabled through {@link StructuredOutputOptions}. They may contain
 * sensitive model output and are truncated to a configured maximum length.
 *
 * @param lastRawContentSnippet truncated raw model output from the last failed attempt; {@code null} when not captured
 * @param lastRepairedContentSnippet truncated repaired output from the last failed attempt; {@code null} when repair was
 * not attempted or content was not captured
 */
public record FailureSnippets(
    String lastRawContentSnippet,
    String lastRepairedContentSnippet
) {

    /**
     * Shared empty snippets with both fields set to {@code null}.
     */
    public static final FailureSnippets EMPTY = new FailureSnippets(null, null);
}
