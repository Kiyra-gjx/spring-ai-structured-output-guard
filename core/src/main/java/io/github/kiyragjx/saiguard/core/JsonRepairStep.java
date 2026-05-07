package io.github.kiyragjx.saiguard.core;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * One step in a JSON repair chain.
 * <p>
 * Implementations should be deterministic, conservative, and avoid semantic rewrites. A step must return a non-null
 * string; returning {@code null} causes {@link JsonRepairer} to fail fast.
 */
@FunctionalInterface
public interface JsonRepairStep {

    /**
     * Repairs the current candidate text.
     *
     * @param rawContent current candidate from the previous step; expected to be non-null
     * @return repaired candidate; must not be {@code null}
     */
    String repair(String rawContent);

    /**
     * Returns a stable name used in logs, metrics, and repair failure callbacks.
     *
     * @return step name; defaults to the implementation class simple name
     */
    default String name() {
        String simpleName = getClass().getSimpleName();
        return simpleName == null || simpleName.isBlank() ? getClass().getName() : simpleName;
    }

    /**
     * Creates a named step from a unary string operator.
     *
     * @param name stable non-blank step name used for observability
     * @param repairer repair function; must not be {@code null} and must return non-null values
     * @return named repair step
     * @throws IllegalArgumentException if {@code name} is blank
     */
    static JsonRepairStep named(String name, UnaryOperator<String> repairer) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(repairer, "repairer cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        return new JsonRepairStep() {
            @Override
            public String repair(String rawContent) {
                return repairer.apply(rawContent);
            }

            @Override
            public String name() {
                return name;
            }
        };
    }
}
