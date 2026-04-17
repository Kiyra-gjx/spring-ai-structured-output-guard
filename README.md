# Spring AI Structured Output Guard

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M1-6DB33F)](https://spring.io/projects/spring-ai)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

A small guard layer for Spring AI structured output calls.

`spring-ai-structured-output-guard` adds a small guard layer around Spring AI structured output flows. It retries only when the failure looks like a parsing problem and applies lightweight JSON cleanup before giving up.

External integration has been verified with a standalone Maven project using the published Maven Central coordinates and an OpenAI-compatible Chat Completions API.

## Why This Exists

Spring AI already gives you `BeanOutputConverter`, and it works well when the model returns valid JSON.

This library is for the cases where the response is close to JSON, but not quite. Common examples are:

- JSON wrapped in Markdown code fences
- trailing commas before `}` or `]`
- leading or trailing prose around the JSON body
- raw newlines and control characters inside JSON strings
- parse failures that need one targeted retry instead of a full custom recovery flow

These responses are often recoverable. Without a small guard layer, they usually turn into repeated parsing and retry code in application services.

### Typical broken responses

````text
```json
{
  "name": "Alice",
  "skills": ["Java", "Spring",],
}
```
````

Or this:

```text
Here is the result you asked for:
{"name":"Alice","summary":"line1
line2"}
```

## What It Does

- retries only when the failure looks like a structured output parsing problem
- applies lightweight JSON cleanup for common low-risk issues
- extracts the actual JSON body from noisy responses
- cleans up trailing commas and normalizes smart quotes
- escapes raw control characters inside JSON strings
- keeps call sites small and typed
- provides a Spring Boot starter for Spring AI projects

## Quick Example

**Without the guard**

```java
BeanOutputConverter<MovieReview> converter = new BeanOutputConverter<>(MovieReview.class);

try {
    String raw = chatClient.prompt()
        .system(systemPrompt + "\n" + converter.getFormat())
        .user(userPrompt)
        .call()
        .content();
    return converter.convert(raw);
} catch (Exception e) {
    // retry?
    // repair json?
    // log?
    // wrap exception?
    throw e;
}
```

**With the guard**

```java
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
```

## Modules

- `core`
  Framework-agnostic retry, repair, parsing orchestration, and tests.
- `starter`
  Spring Boot auto-configuration plus Spring AI integration entry point.
- `example`
  Runnable demo application.

## Installation

The starter is published on Maven Central.

Current published version:

```text
0.1.0
```

Coordinates:

### Gradle

```groovy
implementation "io.github.kiyra-gjx:spring-ai-structured-output-guard-starter:0.1.0"
```

### Maven

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quick Start

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

### Properties

| Property | Default | Description |
|---|---:|---|
| `spring.ai.structured-output.guard.max-attempts` | `2` | Total attempts including the first call |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | Adds the sanitized parse error to retry instructions |
| `spring.ai.structured-output.guard.enable-repair` | `true` | Enables lightweight JSON repair before retrying |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | Truncates parse errors included in retry prompts |

## Repair Strategy

The current repair layer intentionally stays conservative. It does not try to become a full JSON healing engine. It only handles common low-risk cleanup:

- strip UTF-8 BOM
- strip Markdown code fences
- extract the first JSON object or array
- normalize smart quotes
- remove trailing commas before `}` or `]`
- escape raw `\n`, `\r`, `\t`, and control characters inside JSON strings

If the response is fundamentally wrong, the guard still fails fast with `StructuredOutputException`.

## Example Application

The `example` module exposes:

```text
GET /demo/movie-review?movie=Interstellar
```

Run it with:

```bash
./gradlew :example:bootRun
```

Then call:

```bash
curl "http://localhost:8088/demo/movie-review?movie=Interstellar"
```

On Windows PowerShell, prefer `curl.exe` or `Invoke-RestMethod` instead of the `curl` alias.

## Local Development

```bash
./gradlew test
./gradlew :example:bootRun
```

## Compatibility

The project is currently built and tested against:

- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`
- Java `21`

The current release line targets Spring AI `2.0.0-M1`. Moving to a later stable Spring AI line is still a reasonable follow-up once the API surface settles.

## Release Status

`0.1.0` is now published.

The current follow-up work is:

1. keep the public API surface stable across `0.1.x`
2. track compatibility with later Spring AI lines
3. add metrics and extension points in `0.2.x`

See [CHANGELOG.md](./CHANGELOG.md) for release notes.

## Roadmap

- custom repair strategies
- Micrometer metrics hooks
- streaming aggregation support
- richer classification for converter and parser failures
- more end-to-end samples

## Contributing

Issues and pull requests are welcome. See [CONTRIBUTING.md](./CONTRIBUTING.md).
