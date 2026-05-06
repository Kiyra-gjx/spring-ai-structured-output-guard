package io.github.kiyragjx.saiguard.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.ai.structured-output.guard")
public class StructuredOutputGuardProperties {

    /**
     * Total attempts including the first call.
     */
    private int maxAttempts = 2;

    /**
     * Adds the sanitized parse error to retry instructions.
     */
    private boolean includeLastErrorInRetryPrompt = true;

    /**
     * Enables lightweight JSON repair before retrying.
     */
    private boolean enableRepair = true;

    /**
     * Truncates parse errors included in retry prompts.
     */
    private int maxErrorMessageLength = 200;

    /**
     * Retries errors classified as structured-output parsing failures while attempts remain.
     */
    private boolean retryOnStructuredOutputError = true;

    /**
     * Retries errors that are not classified as structured-output parsing failures.
     */
    private boolean retryOnOtherError = false;

    /**
     * Fixed wait before each retry; 0 means no wait.
     */
    private long retryBackoffMillis = 0;

    private final Metrics metrics = new Metrics();

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isIncludeLastErrorInRetryPrompt() {
        return includeLastErrorInRetryPrompt;
    }

    public void setIncludeLastErrorInRetryPrompt(boolean includeLastErrorInRetryPrompt) {
        this.includeLastErrorInRetryPrompt = includeLastErrorInRetryPrompt;
    }

    public boolean isEnableRepair() {
        return enableRepair;
    }

    public void setEnableRepair(boolean enableRepair) {
        this.enableRepair = enableRepair;
    }

    public int getMaxErrorMessageLength() {
        return maxErrorMessageLength;
    }

    public void setMaxErrorMessageLength(int maxErrorMessageLength) {
        this.maxErrorMessageLength = maxErrorMessageLength;
    }

    public boolean isRetryOnStructuredOutputError() {
        return retryOnStructuredOutputError;
    }

    public void setRetryOnStructuredOutputError(boolean retryOnStructuredOutputError) {
        this.retryOnStructuredOutputError = retryOnStructuredOutputError;
    }

    public boolean isRetryOnOtherError() {
        return retryOnOtherError;
    }

    public void setRetryOnOtherError(boolean retryOnOtherError) {
        this.retryOnOtherError = retryOnOtherError;
    }

    public long getRetryBackoffMillis() {
        return retryBackoffMillis;
    }

    public void setRetryBackoffMillis(long retryBackoffMillis) {
        this.retryBackoffMillis = retryBackoffMillis;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public static class Metrics {

        /**
         * Enables the Micrometer listener when a MeterRegistry bean is present.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
