package io.github.kiyragjx.saiguard.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Global Spring Boot configuration properties for the structured-output guard starter.
 */
@ConfigurationProperties("spring.ai.structured-output.guard")
public class StructuredOutputGuardProperties {

    /**
     * Creates properties with starter defaults.
     */
    public StructuredOutputGuardProperties() {
    }

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

    private final FailureSnippets failureSnippets = new FailureSnippets();

    /**
     * Returns total attempts including the first call.
     *
     * @return total attempt count
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Sets total attempts including the first call.
     *
     * @param maxAttempts total attempt count
     */
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Returns whether retry prompts include the sanitized previous parse error.
     *
     * @return {@code true} when previous parse errors are included
     */
    public boolean isIncludeLastErrorInRetryPrompt() {
        return includeLastErrorInRetryPrompt;
    }

    /**
     * Sets whether retry prompts include the sanitized previous parse error.
     *
     * @param includeLastErrorInRetryPrompt {@code true} to include previous parse errors
     */
    public void setIncludeLastErrorInRetryPrompt(boolean includeLastErrorInRetryPrompt) {
        this.includeLastErrorInRetryPrompt = includeLastErrorInRetryPrompt;
    }

    /**
     * Returns whether local JSON repair is enabled.
     *
     * @return {@code true} when repair is enabled
     */
    public boolean isEnableRepair() {
        return enableRepair;
    }

    /**
     * Sets whether local JSON repair is enabled.
     *
     * @param enableRepair {@code true} to enable repair
     */
    public void setEnableRepair(boolean enableRepair) {
        this.enableRepair = enableRepair;
    }

    /**
     * Returns the maximum sanitized parse-error length for retry prompts.
     *
     * @return maximum error-message length
     */
    public int getMaxErrorMessageLength() {
        return maxErrorMessageLength;
    }

    /**
     * Sets the maximum sanitized parse-error length for retry prompts.
     *
     * @param maxErrorMessageLength maximum error-message length
     */
    public void setMaxErrorMessageLength(int maxErrorMessageLength) {
        this.maxErrorMessageLength = maxErrorMessageLength;
    }

    /**
     * Returns whether structured-output parsing errors are retryable while attempts remain.
     *
     * @return {@code true} when structured-output errors are retryable
     */
    public boolean isRetryOnStructuredOutputError() {
        return retryOnStructuredOutputError;
    }

    /**
     * Sets whether structured-output parsing errors are retryable while attempts remain.
     *
     * @param retryOnStructuredOutputError {@code true} to retry structured-output errors
     */
    public void setRetryOnStructuredOutputError(boolean retryOnStructuredOutputError) {
        this.retryOnStructuredOutputError = retryOnStructuredOutputError;
    }

    /**
     * Returns whether non-structured-output errors are retryable while attempts remain.
     *
     * @return {@code true} when other errors are retryable
     */
    public boolean isRetryOnOtherError() {
        return retryOnOtherError;
    }

    /**
     * Sets whether non-structured-output errors are retryable while attempts remain.
     *
     * @param retryOnOtherError {@code true} to retry other errors
     */
    public void setRetryOnOtherError(boolean retryOnOtherError) {
        this.retryOnOtherError = retryOnOtherError;
    }

    /**
     * Returns the fixed wait before each retry.
     *
     * @return retry backoff in milliseconds
     */
    public long getRetryBackoffMillis() {
        return retryBackoffMillis;
    }

    /**
     * Sets the fixed wait before each retry.
     *
     * @param retryBackoffMillis retry backoff in milliseconds
     */
    public void setRetryBackoffMillis(long retryBackoffMillis) {
        this.retryBackoffMillis = retryBackoffMillis;
    }

    /**
     * Returns Micrometer integration properties.
     *
     * @return metrics properties
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Returns failure content snippet properties.
     *
     * @return failure-snippets properties
     */
    public FailureSnippets getFailureSnippets() {
        return failureSnippets;
    }

    /**
     * Micrometer integration properties.
     */
    public static class Metrics {

        /**
         * Creates metrics properties with starter defaults.
         */
        public Metrics() {
        }

        /**
         * Enables the Micrometer listener when a MeterRegistry bean is present.
         */
        private boolean enabled = true;

        /**
         * Returns whether the Micrometer listener is enabled when a MeterRegistry bean is present.
         *
         * @return {@code true} when metrics are enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the Micrometer listener is enabled when a MeterRegistry bean is present.
         *
         * @param enabled {@code true} to enable metrics
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Failure content snippet properties.
     */
    public static class FailureSnippets {

        /**
         * Creates failure-snippets properties with starter defaults.
         */
        public FailureSnippets() {
        }

        /**
         * Captures truncated raw and repaired content in the final exception context.
         */
        private boolean enabled = false;

        /**
         * Maximum length for captured content snippets.
         */
        private int maxLength = 500;

        /**
         * Returns whether failure content snippets are captured.
         *
         * @return {@code true} when snippets are enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether failure content snippets are captured.
         *
         * @param enabled {@code true} to enable snippets
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the maximum length for captured content snippets.
         *
         * @return maximum snippet length
         */
        public int getMaxLength() {
            return maxLength;
        }

        /**
         * Sets the maximum length for captured content snippets.
         *
         * @param maxLength maximum snippet length
         */
        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }
}
