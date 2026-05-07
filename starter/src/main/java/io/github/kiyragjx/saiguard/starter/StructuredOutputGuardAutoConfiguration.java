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

/**
 * Auto-configuration for the Spring AI structured-output guard starter.
 */
@AutoConfiguration
@EnableConfigurationProperties(StructuredOutputGuardProperties.class)
public class StructuredOutputGuardAutoConfiguration {

    /**
     * Creates auto-configuration.
     */
    public StructuredOutputGuardAutoConfiguration() {
    }

    /**
     * Provides the default structured-output error classifier.
     *
     * @return default classifier
     */
    @Bean
    @ConditionalOnMissingBean
    public StructuredOutputErrorClassifier structuredOutputErrorClassifier() {
        return new StructuredOutputErrorClassifier();
    }

    /**
     * Provides the default JSON repairer and appends ordered custom repair steps.
     *
     * @param repairSteps optional custom repair steps from the Spring context
     * @return repairer with built-in steps followed by custom steps
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRepairer jsonRepairer(ObjectProvider<JsonRepairStep> repairSteps) {
        List<JsonRepairStep> steps = new ArrayList<>(JsonRepairer.defaultSteps());
        steps.addAll(repairSteps.orderedStream().toList());
        return new JsonRepairer(steps);
    }

    /**
     * Provides the core structured-output executor configured from starter properties.
     *
     * @param properties bound starter properties
     * @param errorClassifier classifier used for retry decisions
     * @param jsonRepairer repairer used before retrying
     * @param executionListeners optional execution listeners from the Spring context
     * @return configured executor
     */
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
            .retryOnStructuredOutputError(properties.isRetryOnStructuredOutputError())
            .retryOnOtherError(properties.isRetryOnOtherError())
            .retryBackoffMillis(properties.getRetryBackoffMillis())
            .build();
        return new StructuredOutputExecutor(
            options,
            errorClassifier,
            jsonRepairer,
            new CompositeStructuredOutputExecutionListener(executionListeners.orderedStream().toList())
        );
    }

    /**
     * Provides the Spring AI guard facade for application code.
     *
     * @param executor configured core executor
     * @return guard facade
     */
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
