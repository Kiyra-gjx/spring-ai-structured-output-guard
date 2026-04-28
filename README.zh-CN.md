# Spring AI Structured Output Guard 🛡️

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

**别让一条格式不规范的模型响应，直接拖垮你的结构化输出链路。**

在生产环境里使用 Spring AI 的 `BeanOutputConverter` 时，模型仍然可能偶尔返回带 Markdown 代码块的 JSON、带尾逗号的 JSON，或者混着解释性文本的 JSON。与其在业务代码里重复写清洗、重试和异常包装逻辑，不如把这层工作下沉到 guard 层。

## 🚀 它解决了什么

当模型返回的是“差一点就合法”的 JSON 时，guard 会先尝试本地修复，再决定是否值得发起定向重试：

- 代码块包装
  在解析前剥离 Markdown code fence。
- 杂音包裹
  从解释性文本里提取第一个 JSON object 或 array。
- 轻微语法问题
  修复尾逗号、智能引号，以及 JSON 字符串里的原始控制字符。
- 解析失败
  只有当剩余错误仍然像结构化输出解析问题时才重试。

更详细的对比和 malformed JSON 样例矩阵见 [docs/adoption-notes.md](./docs/adoption-notes.md)。

## 🛠️ 快速接入

### 1. 添加依赖

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2. 像 `ChatClient` 的配套组件一样调用

guard 封装了“调用模型 -> 解析 -> 必要时修复 -> 必要时重试”整条链路，所以你不需要手动处理 `converter.convert()` 和异常分类。

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
| 原生 structured output | 你的模型提供方官方支持，而且你希望优先依赖它 |
| 原生 `BeanOutputConverter` | 模型通常返回合法 JSON，不需要恢复层 |
| `StructuredOutputValidationAdvisor` | 你需要 schema 校验和重复尝试，但问题不在 JSON 清洗 |
| Spring AI Guard | 你的真实问题是 code fence、尾逗号、包裹性文本或控制字符这类仍可恢复的 JSON 噪音 |

如果你主要想看详细对比，直接查看 [docs/adoption-notes.md](./docs/adoption-notes.md)。

## ⚙️ 配置

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
        metrics:
          enabled: true
```

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `spring.ai.structured-output.guard.max-attempts` | `2` | 总尝试次数，包含第一次调用 |
| `spring.ai.structured-output.guard.enable-repair` | `true` | 是否在重试前尝试轻量 JSON 修复 |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | 是否把上一次解析错误摘要带入重试提示 |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | 限制重试提示中错误消息的最大长度 |
| `spring.ai.structured-output.guard.metrics.enabled` | `true` | 当存在 `MeterRegistry` Bean 时启用 Micrometer 监听器 |

## 🔧 修复策略

guard 的修复层故意保持保守，只处理低风险、格式性的脏数据：

- 移除 UTF-8 BOM
- 剥离 Markdown code fence
- 提取第一个 JSON object 或 array
- 归一化智能引号
- 删除 `}` 或 `]` 前面的尾逗号
- 转义 JSON 字符串中的原始 `\n`、`\r`、`\t` 和其他控制字符

它不会尝试：

- 猜测缺失的大括号或中括号
- 把单引号伪 JSON 改写成标准 JSON
- 删除 JSON 注释
- 修复语义上已经错误的 payload

这些情况会落入定向重试，或者最终抛出 `StructuredOutputException`。

## 扩展修复能力

如果默认 repair 过程已经接近你的需求，但还不够，你现在有两种扩展方式：

- 添加一个或多个 `JsonRepairStep` Bean，在保留内置 repair 步骤的基础上追加自定义清洗逻辑。
- 提供你自己的 `JsonRepairer` Bean，以移除、重排或完整替换默认步骤链。

### 追加自定义 repair 步骤

自定义 `JsonRepairStep` Bean 会在内置步骤之后执行。在 Spring 应用里，它们按 `@Order` / `Ordered` 顺序应用。

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

### 替换或微调完整步骤链

如果你需要更细粒度的控制，可以自行定义 `JsonRepairer` Bean，并构建你想要的完整步骤链。`JsonRepairer.defaultSteps()` 会暴露当前内置步骤顺序，便于你从默认链开始调整。

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

每个步骤都会接收上一个步骤的输出。如果自定义步骤抛出异常或返回 `null`，`JsonRepairer` 会直接以 `IllegalStateException` fail-fast，而不是悄悄跳过这个坏步骤。

## 可观测性

如果你的应用里已经有 Micrometer `MeterRegistry`，starter 会自动发布结构化输出相关计数器。不需要绑定特定监控后端，任何 Spring Boot 支持的 Micrometer registry 都能接收这些指标。

如果你希望保留 guard 行为但关闭内置 Micrometer 监听器，可以设置 `spring.ai.structured-output.guard.metrics.enabled=false`。

| 指标 | 标签 | 含义 |
|---|---|---|
| `spring.ai.structured.output.guard.calls` | `result=success|repaired_success|failure` | 按最终结果统计完成的 guard 调用总数 |
| `spring.ai.structured.output.guard.repair.attempts` | 无 | 解析失败后进入本地 repair 流程的次数 |
| `spring.ai.structured.output.guard.repair.success` | 无 | repair 后解析成功的次数 |
| `spring.ai.structured.output.guard.retries` | `error_type=structured_output|other` | 被调度的重试次数 |
| `spring.ai.structured.output.guard.failures` | `error_type=structured_output|other` | guard 处理后的最终失败次数 |

`repair.attempts` 统计的是 repair pass 次数，不是顶层请求数。如果同一个请求失败两次、进入两次 repair，再得到最终结果，计数器会增加 `2`。

如果你直接集成的是 `core` 模块而不是 Spring starter，也可以把自己的 `StructuredOutputExecutionListener` 传给 `StructuredOutputExecutor`，把同样的生命周期事件接入你现有的可观测体系。

## 🧱 项目结构

- `core`
  不依赖 Spring 容器的重试、修复与解析编排逻辑。
- `starter`
  Spring Boot 自动配置和开箱即用的集成入口。
- `example`
  可直接运行的 demo 应用。

## 📌 兼容性

截至 2026 年 4 月 28 日，本仓库已实际验证的组合如下：

| Spring Boot | Spring AI | 状态 | 验证路径 |
|---|---|---|---|
| `4.0.1` | `2.0.0-M5` | 已验证 | 默认 `./gradlew test` 路径，覆盖 `core`、`starter` 和 `example` |
| `3.5.11` | `1.1.4` | 已验证 | 使用 `-PspringBootVersion=3.5.11 -PspringAiVersion=1.1.4` 运行兼容性矩阵 |

截至 2026 年 4 月 28 日，Spring AI 当前公开版本线为：

- Stable `1.0.5` 和 `1.1.4`
- Preview `2.0.0-M5`

仓库默认构建现在基于 `Spring Boot 4.0.1 + Spring AI 2.0.0-M5`。

其他 Spring AI 版本线目前不在 CI 兼容性矩阵覆盖范围内，在显式加入并验证之前，都应视为未验证。

## 🤝 贡献与反馈

欢迎提交 issue 或 pull request。

- 发布记录：[CHANGELOG.md](./CHANGELOG.md)
- 贡献说明：[CONTRIBUTING.md](./CONTRIBUTING.md)
