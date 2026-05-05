package io.github.kiyragjx.saiguard.example.demo;

import io.github.kiyragjx.saiguard.core.StructuredOutputExecutor;
import io.github.kiyragjx.saiguard.starter.SpringAiStructuredOutputGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DemoControllerSmokeTest {

    @Test
    void shouldReturnStructuredReviewWhenModelRespondsWithRepairableJson() throws Exception {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("""
            ```json
            {
              "movie": "Interstellar",
              "score": 97,
              "strengths": ["Visuals", "Ambition",],
              "weaknesses": ["Exposition"],
              "summary": "A bold space epic"
            }
            ```
            """);

        DemoController controller = new DemoController(
            builder,
            new SpringAiStructuredOutputGuard(new StructuredOutputExecutor())
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/demo/movie-review").param("movie", "Interstellar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.movie").value("Interstellar"))
            .andExpect(jsonPath("$.score").value(97))
            .andExpect(jsonPath("$.strengths[0]").value("Visuals"))
            .andExpect(jsonPath("$.weaknesses[0]").value("Exposition"))
            .andExpect(jsonPath("$.summary").value("A bold space epic"));
    }

    @Test
    void shouldExposeFailurePathDemosWithoutExternalModelService() throws Exception {
        StructuredOutputExecutor executor = new StructuredOutputExecutor();
        FailurePathDemoService demoService = new FailurePathDemoService(executor);
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new FailurePathDemoController(demoService))
            .build();

        mockMvc.perform(get("/demo/failure-paths"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarios.length()").value(5))
            .andExpect(jsonPath("$.scenarios[0].name").value("markdown-code-fence"))
            .andExpect(jsonPath("$.scenarios[0].outcome").value("repaired"))
            .andExpect(jsonPath("$.scenarios[0].movie").value("Interstellar"))
            .andExpect(jsonPath("$.scenarios[0].modelAttempts").value(1))
            .andExpect(jsonPath("$.scenarios[1].name").value("trailing-comma"))
            .andExpect(jsonPath("$.scenarios[1].outcome").value("repaired"))
            .andExpect(jsonPath("$.scenarios[1].movie").value("Arrival"))
            .andExpect(jsonPath("$.scenarios[1].modelAttempts").value(1))
            .andExpect(jsonPath("$.scenarios[2].name").value("repair-failure-then-retry"))
            .andExpect(jsonPath("$.scenarios[2].outcome").value("retried"))
            .andExpect(jsonPath("$.scenarios[2].movie").value("Dune"))
            .andExpect(jsonPath("$.scenarios[2].modelAttempts").value(2))
            .andExpect(jsonPath("$.scenarios[3].name").value("final-failure-context"))
            .andExpect(jsonPath("$.scenarios[3].outcome").value("failed"))
            .andExpect(jsonPath("$.scenarios[3].failure.attemptCount").value(2))
            .andExpect(jsonPath("$.scenarios[3].failure.repairAttempted").value(true))
            .andExpect(jsonPath("$.scenarios[3].failure.repairSucceeded").value(false))
            .andExpect(jsonPath("$.scenarios[3].failure.errorType").value("structured_output"))
            .andExpect(jsonPath("$.scenarios[4].name").value("per-call-disable-repair"))
            .andExpect(jsonPath("$.scenarios[4].outcome").value("failed-fast"))
            .andExpect(jsonPath("$.scenarios[4].modelAttempts").value(1))
            .andExpect(jsonPath("$.scenarios[4].failure.attemptCount").value(1))
            .andExpect(jsonPath("$.scenarios[4].failure.repairAttempted").value(false))
            .andExpect(jsonPath("$.scenarios[4].failure.errorType").value("structured_output"));
    }
}
