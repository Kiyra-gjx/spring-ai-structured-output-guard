package io.github.kiyragjx.saiguard.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeStructuredOutputExecutionListenerTest {

    @Test
    void shouldIgnoreListenerFailuresAndContinueDispatching() {
        List<String> events = new ArrayList<>();
        CompositeStructuredOutputExecutionListener listener = new CompositeStructuredOutputExecutionListener(List.of(
            new StructuredOutputExecutionListener() {
                @Override
                public void onSuccess(String logContext, int attempts, boolean repaired) {
                    throw new IllegalStateException("boom");
                }
            },
            new StructuredOutputExecutionListener() {
                @Override
                public void onSuccess(String logContext, int attempts, boolean repaired) {
                    events.add(logContext + ":" + attempts + ":" + repaired);
                }
            }
        ));

        listener.onSuccess("resume-task", 2, true);

        assertEquals(List.of("resume-task:2:true"), events);
    }
}
