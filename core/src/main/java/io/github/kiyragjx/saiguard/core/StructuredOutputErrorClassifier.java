package io.github.kiyragjx.saiguard.core;

import java.util.Locale;
import java.util.Set;

/**
 * Heuristic classifier for errors that look like structured-output parsing failures.
 * <p>
 * The classifier checks the throwable chain for common JSON parser class names and message fragments. It is intentionally
 * lightweight and is used only to decide retry policy boundaries.
 */
public class StructuredOutputErrorClassifier {

    private static final Set<String> KEYWORDS = Set.of(
        "illegal unquoted character",
        "cannot deserialize",
        "unexpected character",
        "unexpected end-of-input",
        "unrecognized token",
        "json parse",
        "jsonmappingexception",
        "jsonparseexception",
        "mismatchedinputexception",
        "end-of-input",
        "cannot construct instance",
        "not valid json"
    );

    /**
     * Creates a classifier with the built-in heuristics.
     */
    public StructuredOutputErrorClassifier() {
    }

    /**
     * Returns whether a throwable looks like a structured-output parsing error.
     *
     * @param throwable throwable to classify; {@code null} returns {@code false}
     * @return {@code true} when the throwable chain contains known JSON parsing signals
     */
    public boolean isStructuredOutputError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof StructuredOutputException) {
                return true;
            }

            String className = current.getClass().getName().toLowerCase(Locale.ROOT);
            if (className.contains("jsonparse") || className.contains("jsonmapping") || className.contains("mismatchedinput")) {
                return true;
            }

            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                for (String keyword : KEYWORDS) {
                    if (normalized.contains(keyword)) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
