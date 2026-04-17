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
}
