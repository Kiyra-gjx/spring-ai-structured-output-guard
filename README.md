# Spring AI Structured Output Guard 🛡️

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

**Don't let one malformed model response take down your structured output flow.**

When you use Spring AI with `BeanOutputConverter` in production, the model can still occasionally return JSON wrapped in Markdown, JSON with trailing commas, or JSON mixed with extra prose. Instead of repeating cleanup logic, retry logic, and exception wrapping in service code, push that work down into a guard layer.

## 🚀 What It Fixes

When the model returns output that is "almost valid JSON", the guard first tries a local repair pass and then decides whether a targeted retry is justified:

- Code fences
  Strips Markdown code fences before parsing.
- Noisy wrappers
  Extracts the first JSON object or array from explanatory text.
- Syntax glitches
  Repairs trailing commas, smart quotes, and raw control characters inside JSON strings.
- Parsing failures
  Retries only when the remaining error still looks like a structured-output parsing problem.

For a side-by-side comparison and a malformed JSON sample matrix, see [docs/adoption-notes.md](./docs/adoption-notes.md).

## 🛠️ Quick Start

### 1. Add the dependency

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2. Call it like a `ChatClient` companion

The guard wraps the full flow of "call model -> parse -> repair if needed -> retry if needed", so you do not need to manually juggle `converter.convert()` and exception classification.

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
        return outputGuard.call(
            chatClient,
            "You are a recruiting assistant.",
            "Resume content:\n" + resumeText,
            ResumeSummary.class,
            StructuredOutputCallOptions.builder()
                .logContext("resume-task")
                .failureMessage("Failed to parse resume summary")
                .build()
        );
    }
}
```

## ⚖️ Choosing The Right Tool

| Option | Best fit |
|---|---|
| Native structured output | Your provider supports it and you want to rely on that first |
| Plain `BeanOutputConverter` | The model usually returns valid JSON and you do not need a recovery layer |
| `StructuredOutputValidationAdvisor` | You want schema validation and repeated attempts, but JSON cleanup is not the problem |
| Spring AI Guard | Your real issue is code fences, trailing commas, wrapper prose, or control characters in otherwise recoverable JSON |

If you mainly want the detailed comparison, go straight to [docs/adoption-notes.md](./docs/adoption-notes.md).

## ⚙️ Configuration

The property prefix is `spring.ai.structured-output.guard`.

```yaml
spring:
  ai:
    structured-output:
      guard:
        max-attempts: 2
        enable-repair: true
        include-last-error-in-retry-prompt: true
        max-error-message-length: 200
```

| Property | Default | Description |
|---|---:|---|
| `spring.ai.structured-output.guard.max-attempts` | `2` | Total attempts including the first call |
| `spring.ai.structured-output.guard.enable-repair` | `true` | Enables lightweight JSON repair before retrying |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | Adds the sanitized parse error to retry instructions |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | Truncates parse errors included in retry prompts |

## 🔧 Repair Strategy

The repair layer stays conservative on purpose. It only handles formatting noise that is low-risk to normalize:

- remove UTF-8 BOM
- strip Markdown code fences
- extract the first JSON object or array
- normalize smart quotes
- remove trailing commas before `}` or `]`
- escape raw `\n`, `\r`, `\t`, and other control characters inside JSON strings

It does not try to:

- guess missing braces or brackets
- rewrite single-quoted pseudo JSON into standard JSON
- strip JSON comments
- repair payloads that are already semantically wrong

Those cases fall through to targeted retry or ultimately raise `StructuredOutputException`.

## 🧱 Project Layout

- `core`
  Retry, repair, and parsing orchestration with no Spring container dependency.
- `starter`
  Spring Boot auto-configuration and the out-of-the-box integration entry point.
- `example`
  A runnable demo application.

## 📌 Compatibility

Current tested baseline for this repository:

- Java `21`
- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`

Current public Spring AI release lines, as of April 17, 2026:

- Stable `1.0.5` and `1.1.4`
- Preview `2.0.0-M4`

`0.1.0` means this project has been verified on the baseline above. It should not be read as a blanket compatibility claim for every current Spring AI line.

## 🤝 Contributing

Issues and pull requests are welcome.

- Release notes: [CHANGELOG.md](./CHANGELOG.md)
- Contribution guide: [CONTRIBUTING.md](./CONTRIBUTING.md)
