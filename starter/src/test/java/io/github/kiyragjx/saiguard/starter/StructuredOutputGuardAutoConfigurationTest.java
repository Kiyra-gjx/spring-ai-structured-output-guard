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
        });
    }

    @Test
    void shouldBindPropertiesIntoExecutorOptions() {
        contextRunner
            .withPropertyValues(
                "spring.ai.structured-output.guard.max-attempts=3",
                "spring.ai.structured-output.guard.include-last-error-in-retry-prompt=false",
                "spring.ai.structured-output.guard.enable-repair=false",
                "spring.ai.structured-output.guard.max-error-message-length=512"
            )
            .run(context -> {
                StructuredOutputExecutor executor = context.getBean(StructuredOutputExecutor.class);
                StructuredOutputOptions options = (StructuredOutputOptions) ReflectionTestUtils.getField(executor, "options");

                assertThat(options).isNotNull();
                assertThat(options.maxAttempts()).isEqualTo(3);
                assertThat(options.includeLastErrorInRetryPrompt()).isFalse();
                assertThat(options.enableRepair()).isFalse();
                assertThat(options.maxErrorMessageLength()).isEqualTo(512);
                assertThat(context.getBean(StructuredOutputGuardProperties.class).getMetrics().isEnabled()).isTrue();
            });
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

                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.calls", "result", "repaired_success"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.calls", "result", "success"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.calls", "result", "failure"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.repair.attempts"))
                    .isEqualTo(4.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.repair.success"))
                    .isEqualTo(1.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.retries", "error_type", "structured_output"))
                    .isEqualTo(2.0);
                assertThat(counterValue(meterRegistry, "spring.ai.structured.output.guard.failures", "error_type", "structured_output"))
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
}
