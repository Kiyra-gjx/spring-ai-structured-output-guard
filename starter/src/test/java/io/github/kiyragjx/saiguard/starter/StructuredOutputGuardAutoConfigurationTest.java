package io.github.kiyragjx.saiguard.starter;

import io.github.kiyragjx.saiguard.core.JsonRepairer;
import io.github.kiyragjx.saiguard.core.JsonRepairStep;
import io.github.kiyragjx.saiguard.core.StructuredOutputErrorClassifier;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecution;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutionListener;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutor;
import io.github.kiyragjx.saiguard.core.StructuredOutputException;
import io.github.kiyragjx.saiguard.core.StructuredOutputOptions;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredOutputGuardAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(StructuredOutputGuardAutoConfiguration.class));

    @Test
    void shouldRegisterDefaultBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(StructuredOutputGuardProperties.class);
            assertThat(context).hasSingleBean(StructuredOutputErrorClassifier.class);
            assertThat(context).hasSingleBean(JsonRepairer.class);
            assertThat(context).hasSingleBean(StructuredOutputExecutor.class);
            assertThat(context).hasSingleBean(SpringAiStructuredOutputGuard.class);
            assertThat(context).doesNotHaveBean(StructuredOutputExecutionListener.class);

            StructuredOutputExecutor executor = context.getBean(StructuredOutputExecutor.class);
            StructuredOutputOptions options = (StructuredOutputOptions) ReflectionTestUtils.getField(executor, "options");

            assertThat(options).isNotNull();
            assertThat(options.maxAttempts()).isEqualTo(2);
            assertThat(options.includeLastErrorInRetryPrompt()).isTrue();
            assertThat(options.enableRepair()).isTrue();
            assertThat(options.maxErrorMessageLength()).isEqualTo(200);
            assertThat(options.retryOnStructuredOutputError()).isTrue();
            assertThat(options.retryOnOtherError()).isFalse();
            assertThat(options.retryBackoffMillis()).isZero();
        });
    }

    @Test
    void shouldBindPropertiesIntoExecutorOptions() {
        contextRunner
            .withPropertyValues(
                "spring.ai.structured-output.guard.max-attempts=3",
                "spring.ai.structured-output.guard.include-last-error-in-retry-prompt=false",
                "spring.ai.structured-output.guard.enable-repair=false",
                "spring.ai.structured-output.guard.max-error-message-length=512",
                "spring.ai.structured-output.guard.retry-on-structured-output-error=false",
                "spring.ai.structured-output.guard.retry-on-other-error=true",
                "spring.ai.structured-output.guard.retry-backoff-millis=75"
            )
            .run(context -> {
                StructuredOutputExecutor executor = context.getBean(StructuredOutputExecutor.class);
                StructuredOutputOptions options = (StructuredOutputOptions) ReflectionTestUtils.getField(executor, "options");

                assertThat(options).isNotNull();
                assertThat(options.maxAttempts()).isEqualTo(3);
                assertThat(options.includeLastErrorInRetryPrompt()).isFalse();
                assertThat(options.enableRepair()).isFalse();
                assertThat(options.maxErrorMessageLength()).isEqualTo(512);
                assertThat(options.retryOnStructuredOutputError()).isFalse();
                assertThat(options.retryOnOtherError()).isTrue();
                assertThat(options.retryBackoffMillis()).isEqualTo(75);
                assertThat(context.getBean(StructuredOutputGuardProperties.class).getMetrics().isEnabled()).isTrue();
            });
    }

    @Test
    void shouldMergeCallOptionsOverExecutorDefaults() {
        RecordingStructuredOutputExecutor executor = new RecordingStructuredOutputExecutor(
            StructuredOutputOptions.builder()
                .maxAttempts(3)
                .includeLastErrorInRetryPrompt(false)
                .enableRepair(true)
                .maxErrorMessageLength(512)
                .strictJsonInstruction("Use strict global JSON.")
                .retryOnStructuredOutputError(true)
                .retryOnOtherError(false)
                .retryBackoffMillis(25)
                .build()
        );
        SpringAiStructuredOutputGuard guard = new SpringAiStructuredOutputGuard(executor);
        TestOutput expected = new TestOutput("ok");
        executor.result = expected;

        TestOutput result = guard.call(null, "Return a test output.", "hi", TestOutput.class,
            StructuredOutputCallOptions.builder()
                .logContext("single-call")
                .failureMessage("single failure")
                .maxAttempts(1)
                .enableRepair(false)
                .retryOnStructuredOutputError(false)
                .retryOnOtherError(true)
                .retryBackoffMillis(100L)
                .build());

        assertThat(result).isSameAs(expected);
        assertThat(executor.execution.logContext()).isEqualTo("single-call");
        assertThat(executor.execution.failureMessage()).isEqualTo("single failure");
        assertThat(executor.callOptions.maxAttempts()).isEqualTo(1);
        assertThat(executor.callOptions.enableRepair()).isFalse();
        assertThat(executor.callOptions.includeLastErrorInRetryPrompt()).isFalse();
        assertThat(executor.callOptions.maxErrorMessageLength()).isEqualTo(512);
        assertThat(executor.callOptions.strictJsonInstruction()).isEqualTo("Use strict global JSON.");
        assertThat(executor.callOptions.retryOnStructuredOutputError()).isFalse();
        assertThat(executor.callOptions.retryOnOtherError()).isTrue();
        assertThat(executor.callOptions.retryBackoffMillis()).isEqualTo(100);
        assertThat(executor.defaultOptions().maxAttempts()).isEqualTo(3);
        assertThat(executor.defaultOptions().enableRepair()).isTrue();
        assertThat(executor.defaultOptions().retryBackoffMillis()).isEqualTo(25);
    }

    @Test
    void shouldUseExecutorDefaultsWhenCallOptionsAreMissing() {
        RecordingStructuredOutputExecutor executor = new RecordingStructuredOutputExecutor(
            StructuredOutputOptions.builder()
                .maxAttempts(4)
                .includeLastErrorInRetryPrompt(false)
                .enableRepair(false)
                .maxErrorMessageLength(300)
                .strictJsonInstruction("Use global JSON only.")
                .retryOnStructuredOutputError(false)
                .retryOnOtherError(true)
                .retryBackoffMillis(40)
                .build()
        );
        SpringAiStructuredOutputGuard guard = new SpringAiStructuredOutputGuard(executor);
        TestOutput expected = new TestOutput("ok");
        executor.result = expected;

        TestOutput result = guard.call(null, "Return a test output.", "hi", TestOutput.class,
            StructuredOutputCallOptions.defaults());

        assertThat(result).isSameAs(expected);
        assertThat(executor.callOptions.maxAttempts()).isEqualTo(4);
        assertThat(executor.callOptions.includeLastErrorInRetryPrompt()).isFalse();
        assertThat(executor.callOptions.enableRepair()).isFalse();
        assertThat(executor.callOptions.maxErrorMessageLength()).isEqualTo(300);
        assertThat(executor.callOptions.strictJsonInstruction()).isEqualTo("Use global JSON only.");
        assertThat(executor.callOptions.retryOnStructuredOutputError()).isFalse();
        assertThat(executor.callOptions.retryOnOtherError()).isTrue();
        assertThat(executor.callOptions.retryBackoffMillis()).isEqualTo(40);
    }

    @Test
    void shouldAppendOrderedCustomRepairStepsToDefaultRepairer() {
        contextRunner
            .withBean("markGuardStep", JsonRepairStep.class,
                () -> new OrderedJsonRepairStep(1, "mark-guard",
                    text -> text.replace("\"guard\"", "\"[[guard]]\"")))
            .withBean("finalizeGuardStep", JsonRepairStep.class,
                () -> new OrderedJsonRepairStep(2, "finalize-guard",
                    text -> text.replace("[[guard]]", "patched")))
            .run(context -> {
                JsonRepairer repairer = context.getBean(JsonRepairer.class);

                assertThat(repairer.repair("""
                    ```json
                    {"name":"guard",}
                    ```
                    """)).isEqualTo("{\"name\":\"patched\"}");
            });
    }

    @Test
    void shouldReuseCustomBeansWhenTheyAreProvided() {
        StructuredOutputErrorClassifier customErrorClassifier = new StructuredOutputErrorClassifier();
        JsonRepairer customJsonRepairer = new JsonRepairer();

        contextRunner
            .withBean(StructuredOutputErrorClassifier.class, () -> customErrorClassifier)
            .withBean(JsonRepairer.class, () -> customJsonRepairer)
            .run(context -> {
                StructuredOutputExecutor executor = context.getBean(StructuredOutputExecutor.class);

                assertThat(context.getBean(StructuredOutputErrorClassifier.class)).isSameAs(customErrorClassifier);
                assertThat(context.getBean(JsonRepairer.class)).isSameAs(customJsonRepairer);
                assertThat(ReflectionTestUtils.getField(executor, "errorClassifier")).isSameAs(customErrorClassifier);
                assertThat(ReflectionTestUtils.getField(executor, "jsonRepairer")).isSameAs(customJsonRepairer);
            });
    }

    @Test
    void shouldRegisterMicrometerListenerWhenMeterRegistryIsAvailable() {
        contextRunner
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .run(context -> {
                assertThat(context).hasSingleBean(StructuredOutputExecutionListener.class);
                assertThat(context.getBean(StructuredOutputExecutionListener.class))
                    .isInstanceOf(MicrometerStructuredOutputExecutionListener.class);
            });
    }

    @Test
    void shouldAllowDisablingMicrometerMetrics() {
        contextRunner
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withPropertyValues("spring.ai.structured-output.guard.metrics.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(StructuredOutputExecutionListener.class);
                assertThat(context.getBean(StructuredOutputGuardProperties.class).getMetrics().isEnabled()).isFalse();
            });
    }

    @Test
    void shouldPublishMicrometerMetricsForRepairRetryAndFailure() {
        contextRunner
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withBean("explodeStep", JsonRepairStep.class, () -> JsonRepairStep.named("explode", text -> {
                if (text.contains("trigger-step-failure")) {
                    throw new IllegalArgumentException("bad repair");
                }
                return text;
            }))
            .run(context -> {
                MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
                StructuredOutputExecutor executor = context.getBean(StructuredOutputExecutor.class);

                String repaired = executor.execute(StructuredOutputExecution.<String>builder()
                    .systemPrompt("Return JSON")
                    .userPrompt("hi")
                    .responder((systemPrompt, userPrompt) -> """
                        ```json
                        {"value":"ok",}
                        ```
                        """)
                    .parser(raw -> {
                        if (!raw.contains("\"value\":\"ok\"") || raw.contains(",}")) {
                            throw new IllegalArgumentException("json parse error");
                        }
                        return raw;
                    })
                    .build());

                assertThat(repaired).isEqualTo("{\"value\":\"ok\"}");

                executor.execute(StructuredOutputExecution.<String>builder()
                    .systemPrompt("Return JSON")
                    .userPrompt("hi")
                    .responder(new StructuredOutputResponderSequence("{\"value\":\"ok\"", "{\"value\":\"ok\"}"))
                    .parser(raw -> {
                        if (!raw.endsWith("}")) {
                            throw new IllegalArgumentException("unexpected end-of-input");
                        }
                        return raw;
                    })
                    .build());

                assertThatThrownBy(() -> executor.execute(StructuredOutputExecution.<String>builder()
                    .systemPrompt("Return JSON")
                    .userPrompt("hi")
                    .responder((systemPrompt, userPrompt) -> "{bad json")
                    .parser(raw -> {
                        throw new IllegalArgumentException("json parse error");
                    })
                    .build())).isInstanceOf(StructuredOutputException.class);

                assertThatThrownBy(() -> executor.execute(StructuredOutputExecution.<String>builder()
                    .systemPrompt("Return JSON")
                    .userPrompt("hi")
                    .responder((systemPrompt, userPrompt) -> "{trigger-step-failure")
                    .parser(raw -> {
                        throw new IllegalArgumentException("json parse error");
                    })
                    .build())).isInstanceOf(StructuredOutputException.class);

                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.calls", "result", "repaired_success"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.calls", "result", "success"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.calls", "result", "failure"))
                    .isEqualTo(2.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.repair.attempts"))
                    .isEqualTo(5.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.repair.success"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.retries", "error_type", "structured_output"))
                    .isEqualTo(2.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.failures", "error_type", "structured_output"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.failures", "error_type", "other"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.repair.step.failures", "step", "explode"))
                    .isEqualTo(1.0);
            });
    }

    private double counterValue(MeterRegistry meterRegistry, String name, String... tags) {
        return meterRegistry.find(name).tags(tags).counter().count();
    }

    private static final class StructuredOutputResponderSequence implements io.github.kiyragjx.saiguard.core.StructuredOutputResponder {

        private final String[] responses;
        private int index;

        private StructuredOutputResponderSequence(String... responses) {
            this.responses = responses;
        }

        @Override
        public String respond(String systemPrompt, String userPrompt) {
            return responses[index++];
        }
    }

    private static final class OrderedJsonRepairStep implements JsonRepairStep, Ordered {

        private final int order;
        private final String name;
        private final UnaryOperator<String> delegate;

        private OrderedJsonRepairStep(int order, String name, UnaryOperator<String> delegate) {
            this.order = order;
            this.name = name;
            this.delegate = delegate;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public String repair(String rawContent) {
            return delegate.apply(rawContent);
        }

        @Override
        public String name() {
            return name;
        }
    }

    private record TestOutput(String value) {
    }

    private static final class RecordingStructuredOutputExecutor extends StructuredOutputExecutor {

        private StructuredOutputExecution<?> execution;
        private StructuredOutputOptions callOptions;
        private Object result;

        private RecordingStructuredOutputExecutor(StructuredOutputOptions defaultOptions) {
            super(defaultOptions, new StructuredOutputErrorClassifier(), new JsonRepairer());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(StructuredOutputExecution<T> execution, StructuredOutputOptions callOptions) {
            this.execution = execution;
            this.callOptions = callOptions;
            return (T) result;
        }
    }
}
