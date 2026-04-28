package io.github.kiyragjx.saiguard.core;

import java.util.Objects;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface JsonRepairStep {

    String repair(String rawContent);

    default String name() {
        String simpleName = getClass().getSimpleName();
        return simpleName == null || simpleName.isBlank() ? getClass().getName() : simpleName;
    }

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
