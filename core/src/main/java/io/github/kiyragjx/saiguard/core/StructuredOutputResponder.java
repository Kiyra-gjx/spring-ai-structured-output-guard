package io.github.kiyragjx.saiguard.core;

@FunctionalInterface
public interface StructuredOutputResponder {

    String respond(String systemPrompt, String userPrompt) throws Exception;
}

