# Spring AI Structured Output Guard

`spring-ai-structured-output-guard` is a small library plus Spring Boot starter for one annoying production problem: LLMs that almost return valid JSON.

It adds a defensive layer around Spring AI structured output calls:

- retries only when the failure looks like a structured output problem
- repairs common JSON issues such as code fences, trailing commas, smart quotes, and raw control characters
- centralizes failure handling and logging
- keeps the call site simple

## Why

Spring AI already gives you `BeanOutputConverter`, which is great when the model behaves.

In real applications, you still hit responses like:

- fenced JSON
- extra prose before or after the JSON body
- trailing commas
- raw newlines inside string values
- slightly malformed text that is one repair step away from valid JSON

This project turns that into a reusable guard instead of duplicating `try/catch + retry + repair` logic across services.

## Modules

- `core`: framework-agnostic retry, repair, and execution logic
- `starter`: Spring Boot auto-configuration and Spring AI integration
- `example`: runnable demo application

## Quick Example

```java
@Service
public class ResumeService {

    private final ChatClient chatClient;
    private final SpringAiStructuredOutputGuard outputGuard;

    public ResumeService(ChatClient.Builder chatClientBuilder,
                         SpringAiStructuredOutputGuard outputGuard) {
        this.chatClient = chatClientBuilder.build();
        this.outputGuard = outputGuard;
    }

    public ResumeSummary summarize(String resumeText) {
        String systemPrompt = """
            You are a recruiting assistant.
            Return a concise structured summary of the resume.
            """;

        String userPrompt = "Resume:\n" + resumeText;

        return outputGuard.call(
            chatClient,
            systemPrompt,
            userPrompt,
            ResumeSummary.class,
            StructuredOutputCallOptions.builder()
                .logContext("resume-summary")
                .failureMessage("Failed to parse resume summary")
                .build()
        );
    }
}
```

## Dependency

```groovy
implementation "io.github.kiyragjx:spring-ai-structured-output-guard-starter:0.1.0-SNAPSHOT"
```

## Configuration

```yaml
spring:
  ai:
    structured-output:
      guard:
        max-attempts: 2
        include-last-error-in-retry-prompt: true
        enable-repair: true
        max-error-message-length: 200
```

## Example App

The `example` module exposes a simple endpoint:

```text
GET /demo/movie-review?movie=Interstellar
```

With the right Spring AI model configuration, it returns a typed JSON response parsed through the guard.

## Local Build

```bash
./gradlew test
./gradlew :example:bootRun
```

## Current Stack

This repository is scaffolded against the versions already available in the local workspace cache:

- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`
- Java `21`

If you plan to publish it publicly, upgrading to the latest stable Spring AI line is recommended after the API is validated.

## Roadmap

- support custom repair strategies
- add Micrometer metrics hooks
- support structured output guard around streaming completion aggregation
- add integration tests against a fake chat model

