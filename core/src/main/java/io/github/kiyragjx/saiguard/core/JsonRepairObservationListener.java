package io.github.kiyragjx.saiguard.core;

/**
 * Observes failures from individual {@link JsonRepairStep} instances.
 */
@FunctionalInterface
public interface JsonRepairObservationListener {

    /**
     * Called when a repair step throws or returns {@code null}.
     *
     * @param stepName name of the failing step
     * @param error failure exposed by {@link JsonRepairer}
     */
    void onStepFailed(String stepName, RuntimeException error);
}
