# Spring AI Structured Output Guard

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M1-6DB33F)](https://spring.io/projects/spring-ai)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

面向 Spring AI 的结构化输出防御层。

`spring-ai-structured-output-guard` 用来包装 Spring AI 的结构化输出调用，提供更有针对性的重试、轻量 JSON 修复，以及一个即插即用的 Spring Boot Starter。它解决的是生产环境里常见的那类问题：模型返回的内容“几乎”是合法 JSON，但还差一点，足以让程序在解析时直接失败。

## 这个项目解决什么问题

Spring AI 已经提供了 `BeanOutputConverter`，模型表现稳定时体验很好。

但在真实业务里，你仍然会遇到这些返回：

````text
```json
{
  "name": "Alice",
  "skills": ["Java", "Spring",],
}
```
````

或者：

```text
Here is the result you asked for:
{"name":"Alice","summary":"line1
line2"}
```

这两类内容距离合法 JSON 只差一步，但“差一点”在服务端就足够造成失败。

## 功能概览

- 只在错误看起来像“结构化输出解析失败”时才重试
- 去掉 Markdown code fence 和无关前后缀
- 从噪声文本中提取 JSON 主体
- 去除尾随逗号
- 规范化智能引号
- 转义 JSON 字符串中的原始控制字符
- 保持调用端代码简洁、类型安全
- 提供 Spring Boot 自动配置，开箱即用

## Before / After

**Before**

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
    throw e;
}
```

**After**

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

## 模块结构

- `core`
  与框架无关的重试、修复、解析编排逻辑。
- `starter`
  Spring Boot 自动配置和 Spring AI 集成入口。
- `example`
  可直接运行的演示应用。

## 安装

### Gradle

```groovy
implementation "io.github.kiyragjx:spring-ai-structured-output-guard-starter:0.1.0-SNAPSHOT"
```

### Maven

```xml
<dependency>
  <groupId>io.github.kiyragjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 快速开始

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

## 配置

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

### 配置项

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `spring.ai.structured-output.guard.max-attempts` | `2` | 总尝试次数，包含第一次调用 |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | 是否把上一次解析错误摘要带进重试提示词 |
| `spring.ai.structured-output.guard.enable-repair` | `true` | 是否在重试前启用轻量 JSON 修复 |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | 限制重试提示中错误信息的最大长度 |

## 修复策略

当前修复层刻意保持保守，它不是通用 JSON 修复引擎，只处理风险较低的常见问题：

- 去除 UTF-8 BOM
- 去除 Markdown code fence
- 提取首个 JSON 对象或数组
- 规范化智能引号
- 删除 `}` 或 `]` 前的尾随逗号
- 转义 JSON 字符串中的 `\n`、`\r`、`\t` 和其他控制字符

如果响应本身严重错误，库仍然会抛出 `StructuredOutputException`，而不是硬猜内容。

## 示例应用

`example` 模块提供了这个接口：

```text
GET /demo/movie-review?movie=Interstellar
```

运行方式：

```bash
./gradlew :example:bootRun
```

调用示例：

```bash
curl "http://localhost:8088/demo/movie-review?movie=Interstellar"
```

## 本地开发

```bash
./gradlew test
./gradlew :example:bootRun
```

## 兼容性

当前仓库先基于本地已可用的版本搭建：

- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`
- Java `21`

如果后续要正式公开发布，建议在 API 形态稳定后切到 Spring AI 的最新稳定版。

## 发布计划

计划中的发布步骤：

1. 稳定 API 命名和包结构
2. 增加 fake chat model 集成测试
3. 发布 `0.1.0`
4. 在 `0.2.x` 增加指标和扩展点

详细记录见 [CHANGELOG.md](./CHANGELOG.md)。

## Roadmap

- 支持自定义 repair strategy
- 增加 Micrometer 指标埋点
- 支持流式聚合后的结构化输出防护
- 更丰富的错误分类
- 增加端到端样例

## 参与贡献

欢迎提 issue 和 PR。贡献说明见 [CONTRIBUTING.md](./CONTRIBUTING.md)。

