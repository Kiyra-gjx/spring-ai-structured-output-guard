package io.github.kiyragjx.saiguard.starter;

import io.github.kiyragjx.saiguard.core.JsonRepairer;
import io.github.kiyragjx.saiguard.core.StructuredOutputErrorClassifier;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutor;
import io.github.kiyragjx.saiguard.core.StructuredOutputOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

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
}
