package io.github.kiyragjx.saiguard.core;

@FunctionalInterface
public interface JsonRepairObservationListener {

    void onStepFailed(String stepName, RuntimeException error);
}
