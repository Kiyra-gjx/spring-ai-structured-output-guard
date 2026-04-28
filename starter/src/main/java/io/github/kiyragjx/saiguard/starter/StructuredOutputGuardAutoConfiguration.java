package io.github.kiyragjx.saiguard.starter;

import io.github.kiyragjx.saiguard.core.CompositeStructuredOutputExecutionListener;
import io.github.kiyragjx.saiguard.core.JsonRepairer;
import io.github.kiyragjx.saiguard.core.JsonRepairStep;
import io.github.kiyragjx.saiguard.core.StructuredOutputErrorClassifier;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutionListener;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutor;
import io.github.kiyragjx.saiguard.core.StructuredOutputOptions;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(StructuredOutputGuardProperties.class)
public class StructuredOutputGuardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StructuredOutputErrorClassifier structuredOutputErrorClassifier() {
        return new StructuredOutputErrorClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRepairer jsonRepairer(ObjectProvider<JsonRepairStep> repairSteps) {
        List<JsonRepairStep> steps = new ArrayList<>(JsonRepairer.defaultSteps());
        steps.addAll(repairSteps.orderedStream().toList());
        return new JsonRepairer(steps);
    }

    @Bean
    @ConditionalOnMissingBean
    public StructuredOutputExecutor structuredOutputExecutor(
        StructuredOutputGuardProperties properties,
        StructuredOutputErrorClassifier errorClassifier,
        JsonRepairer jsonRepairer,
        ObjectProvider<StructuredOutputExecutionListener> executionListeners
    ) {
        StructuredOutputOptions options = StructuredOutputOptions.builder()
            .maxAttempts(properties.getMaxAttempts())
            .includeLastErrorInRetryPrompt(properties.isIncludeLastErrorInRetryPrompt())
            .enableRepair(properties.isEnableRepair())
            .maxErrorMessageLength(properties.getMaxErrorMessageLength())
            .build();
        return new StructuredOutputExecutor(
            options,
            errorClassifier,
            jsonRepairer,
            new CompositeStructuredOutputExecutionListener(executionListeners.orderedStream().toList())
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringAiStructuredOutputGuard springAiStructuredOutputGuard(StructuredOutputExecutor executor) {
        return new SpringAiStructuredOutputGuard(executor);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    static class MicrometerMetricsConfiguration {

        @Bean
        @ConditionalOnBean(MeterRegistry.class)
        @ConditionalOnProperty(
            prefix = "spring.ai.structured-output.guard.metrics",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
        )
        public StructuredOutputExecutionListener structuredOutputMicrometerExecutionListener(MeterRegistry meterRegistry) {
            return new MicrometerStructuredOutputExecutionListener(meterRegistry);
        }
    }
}
