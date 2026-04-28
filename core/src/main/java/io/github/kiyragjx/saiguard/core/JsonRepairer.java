package io.github.kiyragjx.saiguard.core;

import java.util.List;
import java.util.Objects;

public class JsonRepairer {

    private static final List<JsonRepairStep> DEFAULT_STEPS = List.of(
        JsonRepairStep.named("stripCodeFence", JsonRepairer::stripCodeFence),
        JsonRepairStep.named("extractJsonBody", JsonRepairer::extractJsonBody),
        JsonRepairStep.named("normalizeQuotes", JsonRepairer::normalizeQuotes),
        JsonRepairStep.named("removeTrailingCommas", JsonRepairer::removeTrailingCommas),
        JsonRepairStep.named("escapeControlCharsInJsonStrings", JsonRepairer::escapeControlCharsInJsonStrings)
    );

    private final List<JsonRepairStep> steps;

    public JsonRepairer() {
        this(DEFAULT_STEPS);
    }

    public JsonRepairer(List<JsonRepairStep> steps) {
        Objects.requireNonNull(steps, "steps cannot be null");
        this.steps = List.copyOf(steps);
    }

    public static List<JsonRepairStep> defaultSteps() {
        return DEFAULT_STEPS;
    }

    public String repair(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return rawContent;
        }

        String candidate = stripBom(rawContent).trim();
        for (JsonRepairStep step : steps) {
            candidate = applyStep(step, candidate);
        }
        return candidate;
    }

    private String applyStep(JsonRepairStep step, String candidate) {
        try {
            String repaired = step.repair(candidate);
            if (repaired == null) {
                throw new IllegalStateException("Json repair step '" + step.name() + "' returned null");
            }
            return repaired;
        } catch (IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Json repair step '" + step.name() + "' failed", e);
        }
    }

    private static String stripBom(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private static String stripCodeFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }

        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return text;
        }

        String body = text.substring(firstNewline + 1);
        int fenceEnd = body.lastIndexOf("```");
        if (fenceEnd >= 0) {
            return body.substring(0, fenceEnd).trim();
        }
        return text;
    }

    private static String extractJsonBody(String text) {
        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return text.substring(objectStart, objectEnd + 1);
        }

        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return text.substring(arrayStart, arrayEnd + 1);
        }
        return text;
    }

    private static String normalizeQuotes(String text) {
        return text
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace('\u2018', '\'')
            .replace('\u2019', '\'');
    }

    private static String removeTrailingCommas(String text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (inString) {
                out.append(ch);
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
                out.append(ch);
                continue;
            }

            if (ch == ',') {
                int j = i + 1;
                while (j < text.length() && Character.isWhitespace(text.charAt(j))) {
                    j++;
                }
                if (j < text.length()) {
                    char next = text.charAt(j);
                    if (next == '}' || next == ']') {
                        continue;
                    }
                }
            }

            out.append(ch);
        }
        return out.toString();
    }

    private static String escapeControlCharsInJsonStrings(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!inString) {
                out.append(ch);
                if (ch == '"') {
                    inString = true;
                }
                continue;
            }

            if (escaped) {
                out.append(ch);
                escaped = false;
                continue;
            }

            if (ch == '\\') {
                out.append(ch);
                escaped = true;
                continue;
            }

            if (ch == '"') {
                out.append(ch);
                inString = false;
                continue;
            }

            if (ch == '\n') {
                out.append("\\n");
                continue;
            }
            if (ch == '\r') {
                out.append("\\r");
                continue;
            }
            if (ch == '\t') {
                out.append("\\t");
                continue;
            }
            if (ch < 0x20) {
                out.append(String.format("\\u%04x", (int) ch));
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }
}
