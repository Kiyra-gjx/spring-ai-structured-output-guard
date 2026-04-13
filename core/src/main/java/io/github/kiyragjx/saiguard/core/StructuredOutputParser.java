package io.github.kiyragjx.saiguard.core;

@FunctionalInterface
public interface StructuredOutputParser<T> {

    T parse(String rawContent) throws Exception;
}

