package io.github.kiyragjx.saiguard.starter;

import io.github.kiyragjx.saiguard.core.StructuredOutputExecution;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutor;
import io.github.kiyragjx.saiguard.core.StructuredOutputOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

/**
 * Spring AI entry point for guarded structured-output calls.
 * <p>
 * This class is intended for application code using {@link ChatClient}. It appends the
 * {@link BeanOutputConverter#getFormat()} instruction, calls the model, delegates parsing to Spring AI, and lets the
 * core executor handle repair, retry, and final {@link io.github.kiyragjx.saiguard.core.StructuredOutputException}
 * wrapping.
 */
public class SpringAiStructuredOutputGuard {

    private final StructuredOutputExecutor executor;

    /**
     * Creates a guard backed by a structured-output executor.
     *
     * @param executor executor configured by auto-configuration or user code; must not be {@code null}
     */
    public SpringAiStructuredOutputGuard(StructuredOutputExecutor executor) {
        this.executor = executor;
    }

    /**
     * Calls Spring AI and parses the response into the target class using default call options.
     *
     * @param chatClient client used for the model call; must not be {@code null}
     * @param systemPrompt base system prompt; {@code null} is treated as blank before the format instruction is appended
     * @param userPrompt user prompt; must not be {@code null}
     * @param targetType target structured type; must not be {@code null}
     * @param <T> parsed result type
     * @return parsed structured result
     */
    public <T> T call(ChatClient chatClient, String systemPrompt, String userPrompt, Class<T> targetType) {
        return call(chatClient, systemPrompt, userPrompt, targetType, StructuredOutputCallOptions.defaults());
    }

    /**
     * Calls Spring AI and parses the response into the target class using optional per-call overrides.
     *
     * @param chatClient client used for the model call; must not be {@code null}
     * @param systemPrompt base system prompt; {@code null} is treated as blank before the format instruction is appended
     * @param userPrompt user prompt; must not be {@code null}
     * @param targetType target structured type; must not be {@code null}
     * @param callOptions per-call overrides; {@code null} uses {@link StructuredOutputCallOptions#defaults()}
     * @param <T> parsed result type
     * @return parsed structured result
     */
    public <T> T call(
        ChatClient chatClient,
        String systemPrompt,
        String userPrompt,
        Class<T> targetType,
        StructuredOutputCallOptions callOptions
    ) {
        return call(chatClient, systemPrompt, userPrompt, new BeanOutputConverter<>(targetType), callOptions);
    }

    /**
     * Calls Spring AI with a custom converter using default call options.
     *
     * @param chatClient client used for the model call; must not be {@code null}
     * @param systemPrompt base system prompt; {@code null} is treated as blank before the format instruction is appended
     * @param userPrompt user prompt; must not be {@code null}
     * @param outputConverter converter that supplies format instructions and parses model content; must not be
     * {@code null}
     * @param <T> parsed result type
     * @return parsed structured result
     */
    public <T> T call(
        ChatClient chatClient,
        String systemPrompt,
        String userPrompt,
        BeanOutputConverter<T> outputConverter
    ) {
        return call(chatClient, systemPrompt, userPrompt, outputConverter, StructuredOutputCallOptions.defaults());
    }

    /**
     * Calls Spring AI with a custom converter and optional per-call overrides.
     * <p>
     * Unset fields in {@code callOptions} inherit the globally configured executor options.
     *
     * @param chatClient client used for the model call; must not be {@code null}
     * @param systemPrompt base system prompt; {@code null} is treated as blank before the format instruction is appended
     * @param userPrompt user prompt; must not be {@code null}
     * @param outputConverter converter that supplies format instructions and parses model content; must not be
     * {@code null}
     * @param callOptions per-call overrides; {@code null} uses {@link StructuredOutputCallOptions#defaults()}
     * @param <T> parsed result type
     * @return parsed structured result
     */
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
            .retryOnStructuredOutputError(valueOrDefault(
                callOptions.retryOnStructuredOutputError(),
                defaults.retryOnStructuredOutputError()
            ))
            .retryOnOtherError(valueOrDefault(callOptions.retryOnOtherError(), defaults.retryOnOtherError()))
            .retryBackoffMillis(valueOrDefault(callOptions.retryBackoffMillis(), defaults.retryBackoffMillis()))
            .failureSnippetsEnabled(valueOrDefault(
                callOptions.failureSnippetsEnabled(),
                defaults.failureSnippetsEnabled()
            ))
            .failureSnippetsMaxLength(valueOrDefault(
                callOptions.failureSnippetsMaxLength(),
                defaults.failureSnippetsMaxLength()
            ))
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

    private long valueOrDefault(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }
}
