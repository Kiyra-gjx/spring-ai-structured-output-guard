package io.github.kiyragjx.saiguard.starter;

import io.github.kiyragjx.saiguard.core.StructuredOutputExecution;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutor;
import io.github.kiyragjx.saiguard.core.StructuredOutputOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

public class SpringAiStructuredOutputGuard {

    private final StructuredOutputExecutor executor;

    public SpringAiStructuredOutputGuard(StructuredOutputExecutor executor) {
        this.executor = executor;
    }

    public <T> T call(ChatClient chatClient, String systemPrompt, String userPrompt, Class<T> targetType) {
        return call(chatClient, systemPrompt, userPrompt, targetType, StructuredOutputCallOptions.defaults());
    }

    public <T> T call(
        ChatClient chatClient,
        String systemPrompt,
        String userPrompt,
        Class<T> targetType,
        StructuredOutputCallOptions callOptions
    ) {
        return call(chatClient, systemPrompt, userPrompt, new BeanOutputConverter<>(targetType), callOptions);
    }

    public <T> T call(
        ChatClient chatClient,
        String systemPrompt,
        String userPrompt,
        BeanOutputConverter<T> outputConverter
    ) {
        return call(chatClient, systemPrompt, userPrompt, outputConverter, StructuredOutputCallOptions.defaults());
    }

    public <T> T call(
        ChatClient chatClient,
        String systemPrompt,
        String userPrompt,
        BeanOutputConverter<T> outputConverter,
        StructuredOutputCallOptions callOptions
    ) {
        StructuredOutputCallOptions effectiveCallOptions = callOptions == null
            ? StructuredOutputCallOptions.defaults()
            : callOptions;
        String systemPromptWithFormat = appendFormat(systemPrompt, outputConverter.getFormat());

        return executor.execute(StructuredOutputExecution.<T>builder()
            .systemPrompt(systemPromptWithFormat)
            .userPrompt(userPrompt)
            .logContext(effectiveCallOptions.logContext())
            .failureMessage(effectiveCallOptions.failureMessage())
            .responder((attemptSystemPrompt, attemptUserPrompt) -> chatClient.prompt()
                .system(attemptSystemPrompt)
                .user(attemptUserPrompt)
                .call()
                .content())
            .parser(outputConverter::convert)
            .build(), mergeOptions(effectiveCallOptions));
    }

    private String appendFormat(String systemPrompt, String format) {
        String basePrompt = systemPrompt == null ? "" : systemPrompt.trim();
        return basePrompt + "\n\nReturn JSON that follows this format exactly:\n" + format;
    }

    private StructuredOutputOptions mergeOptions(StructuredOutputCallOptions callOptions) {
        StructuredOutputOptions defaults = executor.defaultOptions();
        return StructuredOutputOptions.builder()
            .maxAttempts(valueOrDefault(callOptions.maxAttempts(), defaults.maxAttempts()))
            .includeLastErrorInRetryPrompt(valueOrDefault(
                callOptions.includeLastErrorInRetryPrompt(),
                defaults.includeLastErrorInRetryPrompt()
            ))
            .enableRepair(valueOrDefault(callOptions.enableRepair(), defaults.enableRepair()))
            .maxErrorMessageLength(valueOrDefault(callOptions.maxErrorMessageLength(), defaults.maxErrorMessageLength()))
            .strictJsonInstruction(valueOrDefault(callOptions.strictJsonInstruction(), defaults.strictJsonInstruction()))
            .build();
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private boolean valueOrDefault(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
