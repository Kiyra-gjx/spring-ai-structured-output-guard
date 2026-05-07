package io.github.kiyragjx.saiguard.core;

import java.util.List;
import java.util.Objects;

/**
 * Applies an ordered chain of conservative JSON repair steps.
 * <p>
 * The default steps are intended for low-risk formatting noise such as Markdown code fences, wrapper prose, smart
 * quotes, trailing commas, and raw control characters inside JSON strings. This class is an extension point for direct
 * core users and Spring users who provide a custom {@code JsonRepairer} bean.
 */
public class JsonRepairer {

    private static final List<JsonRepairStep> DEFAULT_STEPS = List.of(
        JsonRepairStep.named("stripCodeFence", JsonRepairer::stripCodeFence),
        JsonRepairStep.named("extractJsonBody", JsonRepairer::extractJsonBody),
        JsonRepairStep.named("normalizeQuotes", JsonRepairer::normalizeQuotes),
        JsonRepairStep.named("removeTrailingCommas", JsonRepairer::removeTrailingCommas),
        JsonRepairStep.named("escapeControlCharsInJsonStrings", JsonRepairer::escapeControlCharsInJsonStrings)
    );

    private final List<JsonRepairStep> steps;

    /**
     * Creates a repairer with the built-in conservative step chain.
     */
    public JsonRepairer() {
        this(DEFAULT_STEPS);
    }

    /**
     * Creates a repairer with a custom ordered step chain.
     *
     * @param steps repair steps to run in order; must not be {@code null} and must not contain {@code null} entries
     */
    public JsonRepairer(List<JsonRepairStep> steps) {
        Objects.requireNonNull(steps, "steps cannot be null");
        this.steps = List.copyOf(steps);
    }

    /**
     * Returns the built-in conservative repair steps.
     * <p>
     * The returned list is immutable and safe to copy when building a custom chain.
     *
     * @return built-in repair steps in execution order
     */
    public static List<JsonRepairStep> defaultSteps() {
        return DEFAULT_STEPS;
    }

    /**
     * Repairs content with no observation callback.
     *
     * @param rawContent content returned by the model; {@code null} or blank content is returned unchanged
     * @return repaired candidate, or the original value when there is nothing to repair
     */
    public String repair(String rawContent) {
        return repair(rawContent, null);
    }

    /**
     * Repairs content and optionally observes step failures.
     * <p>
     * If a step returns {@code null} or throws a runtime exception, repair fails fast with an
     * {@link IllegalStateException}. The full payload is not included in the generated exception message.
     *
     * @param rawContent content returned by the model; {@code null} or blank content is returned unchanged
     * @param observationListener optional listener for step failures; may be {@code null}
     * @return repaired candidate, or the original value when there is nothing to repair
     * @throws IllegalStateException when a repair step fails or returns {@code null}
     */
    public String repair(String rawContent, JsonRepairObservationListener observationListener) {
        if (rawContent == null || rawContent.isBlank()) {
            return rawContent;
        }

        String candidate = stripBom(rawContent).trim();
        for (JsonRepairStep step : steps) {
            candidate = applyStep(step, candidate, observationListener);
        }
        return candidate;
    }

    private String applyStep(
        JsonRepairStep step,
        String candidate,
        JsonRepairObservationListener observationListener
    ) {
        try {
            String repaired = step.repair(candidate);
            if (repaired == null) {
                throw new IllegalStateException("Json repair step '" + step.name() + "' returned null");
            }
            return repaired;
        } catch (IllegalStateException e) {
            notifyStepFailed(observationListener, step, e);
            throw e;
        } catch (RuntimeException e) {
            IllegalStateException wrapped = new IllegalStateException("Json repair step '" + step.name() + "' failed", e);
            notifyStepFailed(observationListener, step, wrapped);
            throw wrapped;
        }
    }

    private void notifyStepFailed(
        JsonRepairObservationListener observationListener,
        JsonRepairStep step,
        RuntimeException error
    ) {
        if (observationListener != null) {
            observationListener.onStepFailed(step.name(), error);
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
