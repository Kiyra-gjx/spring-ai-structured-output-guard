# Spring AI Structured Output Guard 🛡️

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

**别让模型偶发的“手抖”，直接炸掉你的结构化输出。**

在生产环境里使用 Spring AI 的 `BeanOutputConverter` 时，模型即使整体表现稳定，也还是会偶尔返回带 Markdown 代码块、尾逗号、解释性文字或未转义控制字符的 JSON。与其在业务代码里反复写清洗逻辑、重试逻辑和异常包装，不如把这层脏活交给 Guard。

## 🚀 它解决了什么

当模型返回以下这类“差一点就合法”的响应时，Guard 会先尝试本地修复，再决定是否发起定向重试：

- 代码块包裹
  自动剥离 Markdown code fence。
- 脏数据污染
  提取正文里的第一个 JSON object / array，忽略前后解释文字。
- 语法瑕疵
  修复尾逗号、智能引号和 JSON 字符串中的原始控制字符。
- 解析崩溃
  如果本地修复仍然失败，只在错误仍然像结构化输出解析问题时才重试。

更完整的对比和 malformed JSON 样例矩阵见 [docs/adoption-notes.md](./docs/adoption-notes.md)。

## 🛠️ 快速接入

### 1. 引入依赖

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2. 像用 `ChatClient` 一样调用

Guard 封装了“调用模型 -> 解析 -> 必要时修复 -> 必要时重试”这一整条链路，你不需要自己手动做 `converter.convert()` 和异常分类。

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

## ⚖️ 选型建议

| 方案 | 适用场景 |
|---|---|
| 原生 structured output | 模型提供商官方支持，而且你优先依赖该能力 |
| 原生 `BeanOutputConverter` | 模型基本稳定返回合法 JSON，不想引入额外恢复层 |
| `StructuredOutputValidationAdvisor` | 你需要 schema 校验和重复尝试，但问题不在 JSON 清理 |
| Spring AI Guard | 你真正遇到的是 code fence、尾逗号、解释文字、控制字符这类“脏但可救”的响应 |

如果你只是想看这三者的详细区别，直接看 [docs/adoption-notes.md](./docs/adoption-notes.md)。

## ⚙️ 核心配置

配置前缀是 `spring.ai.structured-output.guard`。

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

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `spring.ai.structured-output.guard.max-attempts` | `2` | 总尝试次数，包含第一次调用 |
| `spring.ai.structured-output.guard.enable-repair` | `true` | 是否在重试前尝试轻量 JSON 修复 |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | 是否把上一次解析错误摘要带进重试提示 |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | 限制重试提示里错误信息的最大长度 |

## 🔧 修复策略

Guard 的修复层遵循“保守且安全”的原则，只处理明确的格式干扰：

- 自动移除 UTF-8 BOM
- 剥离 Markdown code fence
- 提取字符串里的第一个 JSON object / array
- 归一化智能引号
- 修正 `}` 或 `]` 前的非法尾逗号
- 转义 JSON 字符串中的原始 `\n`、`\r`、`\t` 和其他控制字符

不会做的事情：

- 猜测缺失的大括号或中括号
- 把单引号伪 JSON 改写成标准 JSON
- 删除 JSON 注释
- 修正语义上已经错误的 payload

这类问题会直接进入定向重试，或者最终抛出 `StructuredOutputException`。

## 🧱 项目结构

- `core`
  纯净的重试、修复与解析编排逻辑，不依赖 Spring 容器。
- `starter`
  Spring Boot 自动配置入口，提供开箱即用的 Bean。
- `example`
  可直接运行的 demo 应用。

## 📌 兼容性

当前仓库实际验证的基线是：

- Java `21`
- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`

截至 2026 年 4 月 17 日，Spring AI 官方公开版本线是：

- Stable `1.0.5` 和 `1.1.4`
- Preview `2.0.0-M4`

`0.1.0` 目前只表示对上面这套基线做过验证，不表示已经覆盖所有当前 Spring AI 版本。

## 🤝 贡献与反馈

欢迎提交 Issue 或 Pull Request。

- 变更记录见 [CHANGELOG.md](./CHANGELOG.md)
- 贡献说明见 [CONTRIBUTING.md](./CONTRIBUTING.md)
