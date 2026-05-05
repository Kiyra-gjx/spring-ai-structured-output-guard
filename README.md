# Spring AI Structured Output Guard 🛡️

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
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

## 🚫 When Not To Use This Guard

Use a different layer when the problem is not low-risk malformed JSON recovery:

- Do not use it as a replacement for provider-native structured output or JSON schema mode. Prefer native structured output when your provider supports it and it fits your contract.
- Do not use it as a replacement for schema validation or business-field validation. Invalid shapes, missing required domain values, or semantically wrong payloads should still be handled by validation close to your data model.
- Do not use it to repair semantically wrong responses, such as a payload that is valid JSON but answers the wrong question.
- Do not rely on it for aggressive JSON guessing, such as inventing missing brackets, rewriting single-quoted pseudo JSON, or stripping JSON comments.
- Do not use it to save or propagate complete model output by default. The guard keeps failure context lightweight; raw or repaired payload snippets should only be exposed through explicit, tightly bounded options.

It is a good fit when you still use prompt-based structured output and the recurring failures are formatting noise such as Markdown code fences, wrapper prose, trailing commas, or raw control characters. It is also useful when you want retry, repair, and exception wrapping to live in the guard layer instead of being repeated across service code.

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
        metrics:
          enabled: true
```

| Property | Default | Description |
|---|---:|---|
| `spring.ai.structured-output.guard.max-attempts` | `2` | Total attempts including the first call |
| `spring.ai.structured-output.guard.enable-repair` | `true` | Enables lightweight JSON repair before retrying |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | Adds the sanitized parse error to retry instructions |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | Truncates parse errors included in retry prompts |
| `spring.ai.structured-output.guard.metrics.enabled` | `true` | Enables the Micrometer listener when a `MeterRegistry` bean is present |

### Per-call overrides

Global properties should stay the main path. When one call needs a different retry or repair posture, set only the fields you want to override on `StructuredOutputCallOptions`; unset fields continue to use the starter's global configuration, and core defaults still apply underneath.

```java
ResumeSummary summary = outputGuard.call(
    chatClient,
    "You are a recruiting assistant.",
    "Resume content:\n" + resumeText,
    ResumeSummary.class,
    StructuredOutputCallOptions.builder()
        .logContext("strict-resume-task")
        .maxAttempts(1)
        .enableRepair(false)
        .includeLastErrorInRetryPrompt(false)
        .build()
);
```

Override precedence is: per-call options > starter global configuration > core defaults. This is useful for low-latency paths, calls that must fail fast, or prompts where local JSON repair would be riskier than returning an error.

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

## Failure Context

When the guard ultimately throws `StructuredOutputException`, callers can inspect a small failure context without parsing logs:

- `attemptCount`
  Number of model attempts completed before failure.
- `repairAttempted`
  Whether the local JSON repair path was entered at least once.
- `repairSucceeded`
  Whether any repair pass produced content that parsed successfully before a later failure.
- `errorType`
  Classified as `structured_output`, `other`, or `unknown`.

The exception exposes these values through `failureContext()` and convenience methods such as `attemptCount()` and `errorType()`. It does not attach raw model output or repaired output snippets by default, so potentially sensitive payloads are not stored on the exception object.

## Extending Repair

If the default repair pass is close but not quite enough for your model or provider, you now have two extension paths:

- Add one or more `JsonRepairStep` beans when you want to keep the built-in repair steps and append custom cleanup logic.
- Provide your own `JsonRepairer` bean when you need to remove, reorder, or fully replace the default step chain.

### Append custom repair steps

Custom `JsonRepairStep` beans run after the built-in steps. In Spring applications they are applied in `@Order` / `Ordered` order.

```java
@Configuration
class StructuredOutputRepairConfig {

    @Bean
    @Order(100)
    JsonRepairStep normalizeAngleQuoteTokens() {
        return JsonRepairStep.named("normalizeAngleQuoteTokens",
            text -> text.replace("<<", "\""));
    }
}
```

### Replace or tune the full step chain

If you need finer control, define a `JsonRepairer` bean yourself and build the exact chain you want. `JsonRepairer.defaultSteps()` exposes the current built-in sequence so you can start from the defaults and adjust it.

```java
@Configuration
class StructuredOutputRepairConfig {

    @Bean
    JsonRepairer jsonRepairer() {
        List<JsonRepairStep> steps = new ArrayList<>(JsonRepairer.defaultSteps());
        steps.removeIf(step -> step.name().equals("normalizeQuotes"));
        steps.add(JsonRepairStep.named("normalizeAngleQuoteTokens",
            text -> text.replace("<<", "\"")));
        return new JsonRepairer(steps);
    }
}
```

Each step receives the previous step's output. If a custom step throws an exception or returns `null`, `JsonRepairer` fails fast with an `IllegalStateException` instead of silently skipping the bad step. The failure message, warning log, and `StructuredOutputExecutionListener.onRepairStepFailed(...)` event include the failing step name, but they do not include the full payload.

## Observability

If your application already exposes a Micrometer `MeterRegistry`, the starter automatically publishes structured output counters. This does not require a specific monitoring backend. Any Micrometer registry supported by Spring Boot can collect them.

Set `spring.ai.structured-output.guard.metrics.enabled=false` if you want to keep the guard behavior but disable the built-in Micrometer listener.

| Metric | Tags | Meaning |
|---|---|---|
| `spring.ai.structured.output.guard.calls` | `result=success|repaired_success|failure` | Total completed guard calls grouped by final outcome |
| `spring.ai.structured.output.guard.repair.attempts` | none | Number of local repair passes entered after parse failures |
| `spring.ai.structured.output.guard.repair.success` | none | Number of times repaired content parsed successfully |
| `spring.ai.structured.output.guard.repair.step.failures` | `step` | Number of repair step failures grouped by step name |
| `spring.ai.structured.output.guard.retries` | `error_type=structured_output|other` | Number of retry attempts scheduled |
| `spring.ai.structured.output.guard.failures` | `error_type=structured_output|other` | Number of final failures after guard processing |

`repair.attempts` counts repair passes, not just top-level requests. If one request fails parsing twice and enters repair twice before the final result, the counter increases by `2`.

If you are integrating `core` directly instead of the Spring starter, you can pass your own `StructuredOutputExecutionListener` to `StructuredOutputExecutor` and forward the same lifecycle events into your observability stack. Override `onRepairStepFailed(logContext, stepName, error)` when you need to identify which custom `JsonRepairStep` failed.

## 🧱 Project Layout

- `core`
  Retry, repair, and parsing orchestration with no Spring container dependency.
- `starter`
  Spring Boot auto-configuration and the out-of-the-box integration entry point.
- `example`
  A runnable demo application. It also exposes `GET /demo/failure-paths`, a local scripted demo that does not need an API key and shows code fence repair, trailing-comma repair, retry after unrecoverable repair, final `StructuredOutputException` failure context, and per-call `maxAttempts` / `enableRepair` overrides.

## 📌 Compatibility

Current verified combinations for this repository, last validated on April 28, 2026:

| Spring Boot | Spring AI | Status | Validation path |
|---|---|---|---|
| `4.0.1` | `2.0.0-M5` | Verified | Default `./gradlew test` path covering `core`, `starter`, and `example` |
| `3.5.11` | `1.1.4` | Verified | Compatibility matrix run with `-PspringBootVersion=3.5.11 -PspringAiVersion=1.1.4` |

Current public Spring AI release lines, as of April 28, 2026:

- Stable `1.0.5` and `1.1.4`
- Preview `2.0.0-M5`

The repository default build now targets `Spring Boot 4.0.1 + Spring AI 2.0.0-M5`.

Other Spring AI lines are not currently covered by the CI compatibility matrix and should be treated as unverified until they are explicitly added and tested.

## 🤝 Contributing

Issues and pull requests are welcome.

- Release notes: [CHANGELOG.md](./CHANGELOG.md)
- Contribution guide: [CONTRIBUTING.md](./CONTRIBUTING.md)
