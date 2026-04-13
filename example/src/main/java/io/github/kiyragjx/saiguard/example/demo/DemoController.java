package io.github.kiyragjx.saiguard.example.demo;

import io.github.kiyragjx.saiguard.starter.SpringAiStructuredOutputGuard;
import io.github.kiyragjx.saiguard.starter.StructuredOutputCallOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    private final ChatClient chatClient;
    private final SpringAiStructuredOutputGuard outputGuard;

    public DemoController(ChatClient.Builder chatClientBuilder, SpringAiStructuredOutputGuard outputGuard) {
        this.chatClient = chatClientBuilder.build();
        this.outputGuard = outputGuard;
    }

    @GetMapping("/demo/movie-review")
    public MovieReview movieReview(@RequestParam(defaultValue = "Interstellar") String movie) {
        String systemPrompt = """
            You are a film critic.
            Return a structured review for the given movie.
            Score must be from 0 to 100.
            """;

        String userPrompt = "Movie: " + movie;

        return outputGuard.call(
            chatClient,
            systemPrompt,
            userPrompt,
            MovieReview.class,
            StructuredOutputCallOptions.builder()
                .logContext("movie-review")
                .failureMessage("Failed to parse movie review")
                .build()
        );
    }
}

