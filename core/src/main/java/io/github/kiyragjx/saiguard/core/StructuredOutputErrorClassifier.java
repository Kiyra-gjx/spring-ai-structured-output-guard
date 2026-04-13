package io.github.kiyragjx.saiguard.core;

import java.util.Locale;
import java.util.Set;

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

