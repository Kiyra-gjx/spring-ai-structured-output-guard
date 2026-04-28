package io.github.kiyragjx.saiguard.starter;

import io.github.kiyragjx.saiguard.core.JsonRepairer;
import io.github.kiyragjx.saiguard.core.JsonRepairStep;
import io.github.kiyragjx.saiguard.core.StructuredOutputErrorClassifier;
import io.github.kiyragjx.saiguard.core.StructuredOutputExecutor;
import io.github.kiyragjx.saiguard.core.StructuredOutputOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
        JsonRepairer jsonRepairer
    ) {
        StructuredOutputOptions options = StructuredOutputOptions.builder()
            .maxAttempts(properties.getMaxAttempts())
            .includeLastErrorInRetryPrompt(properties.isIncludeLastErrorInRetryPrompt())
            .enableRepair(properties.isEnableRepair())
            .maxErrorMessageLength(properties.getMaxErrorMessageLength())
            .build();
        return new StructuredOutputExecutor(options, errorClassifier, jsonRepairer);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringAiStructuredOutputGuard springAiStructuredOutputGuard(StructuredOutputExecutor executor) {
        return new SpringAiStructuredOutputGuard(executor);
    }
}
