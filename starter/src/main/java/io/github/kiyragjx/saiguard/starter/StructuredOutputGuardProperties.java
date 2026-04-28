package io.github.kiyragjx.saiguard.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.ai.structured-output.guard")
public class StructuredOutputGuardProperties {

    private int maxAttempts = 2;
    private boolean includeLastErrorInRetryPrompt = true;
    private boolean enableRepair = true;
    private int maxErrorMessageLength = 200;
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

    public Metrics getMetrics() {
        return metrics;
    }

    public static class Metrics {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
