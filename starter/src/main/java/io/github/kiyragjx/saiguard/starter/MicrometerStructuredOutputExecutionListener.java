package io.github.kiyragjx.saiguard.starter;

import io.github.kiyragjx.saiguard.core.StructuredOutputExecutionListener;
import io.micrometer.core.instrument.MeterRegistry;

class MicrometerStructuredOutputExecutionListener implements StructuredOutputExecutionListener {

    private static final String METRIC_PREFIX = "spring.ai.structured.output.guard";
    private static final String CALLS = METRIC_PREFIX + ".calls";
    private static final String REPAIR_ATTEMPTS = METRIC_PREFIX + ".repair.attempts";
    private static final String REPAIR_SUCCESS = METRIC_PREFIX + ".repair.success";
    private static final String RETRIES = METRIC_PREFIX + ".retries";
    private static final String FAILURES = METRIC_PREFIX + ".failures";

    private final MeterRegistry meterRegistry;

    MicrometerStructuredOutputExecutionListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onRepairAttempted(String logContext) {
        meterRegistry.counter(REPAIR_ATTEMPTS).increment();
    }

    @Override
    public void onRepairSucceeded(String logContext) {
        meterRegistry.counter(REPAIR_SUCCESS).increment();
    }

    @Override
    public void onRetry(String logContext, int attempt, String errorType) {
        meterRegistry.counter(RETRIES, "error_type", safeTag(errorType)).increment();
    }

    @Override
    public void onSuccess(String logContext, int attempts, boolean repaired) {
        meterRegistry.counter(CALLS, "result", repaired ? "repaired_success" : "success").increment();
    }

    @Override
    public void onFailure(String logContext, int attempts, String errorType) {
        meterRegistry.counter(CALLS, "result", "failure").increment();
        meterRegistry.counter(FAILURES, "error_type", safeTag(errorType)).increment();
    }

    private String safeTag(String tagValue) {
        return tagValue == null || tagValue.isBlank() ? "unknown" : tagValue;
    }
}
