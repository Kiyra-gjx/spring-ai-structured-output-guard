# Spring AI Structured Output Guard 🛡️

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

**少し崩れたモデル応答ひとつで、構造化出力フロー全体を壊さないための guard です。**

本番で Spring AI の `BeanOutputConverter` を使っていても、モデルは Markdown の code fence 付き JSON、末尾カンマ付き JSON、説明文が混ざった JSON をときどき返します。サービスごとに毎回クリーニング、再試行、例外ラップを書くより、その役割を guard レイヤーに下ろした方が扱いやすくなります。

## 🚀 何を解決するか

モデル応答が「あと少しで正しい JSON」という状態なら、guard はまずローカル修復を試し、そのうえで定向リトライが妥当かどうかを判断します。

- code fence
  解析前に Markdown code fence を取り除きます。
- 余計なラッパー文
  説明文の中から最初の JSON object / array を抽出します。
- 軽微な構文ノイズ
  末尾カンマ、smart quotes、JSON 文字列内の生の制御文字を修正します。
- 解析失敗
  残ったエラーがまだ構造化出力の解析問題らしい場合にだけ再試行します。

詳しい比較と malformed JSON サンプル行列は [docs/adoption-notes.md](./docs/adoption-notes.md) を参照してください。

## 🛠️ クイックスタート

### 1. 依存関係を追加する

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2. `ChatClient` の相棒として呼び出す

guard は「モデル呼び出し -> 解析 -> 必要なら修復 -> 必要なら再試行」までをまとめて扱うので、`converter.convert()` や例外分類を自分で毎回書く必要はありません。

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

## ⚖️ 使い分け

| 選択肢 | 向いているケース |
|---|---|
| Native structured output | プロバイダーが公式対応しており、まずそこを使いたい |
| 素の `BeanOutputConverter` | モデルがほとんど常に正しい JSON を返し、回復レイヤーが不要 |
| `StructuredOutputValidationAdvisor` | schema 検証と再試行は必要だが、JSON クリーニングは本題ではない |
| Spring AI Guard | code fence、末尾カンマ、説明文混在、制御文字といった“まだ救える JSON”が実際の問題 |

3 つの違いを詳しく見たい場合は [docs/adoption-notes.md](./docs/adoption-notes.md) を参照してください。

## ⚙️ 設定

プロパティプレフィックスは `spring.ai.structured-output.guard` です。

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
| `spring.ai.structured-output.guard.max-attempts` | `2` | 最初の呼び出しを含む総試行回数 |
| `spring.ai.structured-output.guard.enable-repair` | `true` | 再試行前に軽量 JSON 修復を有効にする |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | 前回の解析エラー要約を再試行プロンプトに含める |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | 再試行プロンプトに含めるエラーメッセージ長を制限する |
| `spring.ai.structured-output.guard.metrics.enabled` | `true` | `MeterRegistry` Bean がある場合に Micrometer リスナーを有効にする |

## 🔧 修復ポリシー

修復レイヤーは意図的に保守的です。低リスクで正規化しやすいフォーマットノイズだけを扱います。

- UTF-8 BOM を除去する
- Markdown code fence を剥がす
- 最初の JSON object / array を抽出する
- smart quotes を正規化する
- `}` または `]` の直前にある末尾カンマを削除する
- JSON 文字列内の生 `\n`、`\r`、`\t`、その他の制御文字をエスケープする

次のことは行いません。

- 不足している波括弧や角括弧を推測する
- シングルクォートの擬似 JSON を標準 JSON に書き換える
- JSON コメントを削除する
- すでに意味的に壊れている payload を直す

これらのケースは定向リトライに回るか、最終的に `StructuredOutputException` を返します。

## 修復の拡張

デフォルトの repair パスが近いものの十分ではない場合、拡張方法は 2 つあります。

- 1 つ以上の `JsonRepairStep` Bean を追加し、組み込み repair ステップを残したまま独自のクリーンアップ処理を後ろに追加する。
- 自前の `JsonRepairer` Bean を定義し、デフォルトのステップチェーンを削除・並び替え・完全置換する。

### カスタム repair ステップを追加する

カスタム `JsonRepairStep` Bean は組み込みステップの後に実行されます。Spring アプリケーションでは `@Order` / `Ordered` の順序で適用されます。

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

### ステップチェーン全体を置き換える、または微調整する

より細かい制御が必要な場合は、自前の `JsonRepairer` Bean を定義して、必要なステップチェーンを組み立てます。`JsonRepairer.defaultSteps()` で現在の組み込み順序を取得できるので、そこから調整を始められます。

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

各ステップは前のステップの出力を受け取ります。カスタムステップが例外を投げるか `null` を返した場合、`JsonRepairer` はその壊れたステップを黙って無視せず、`IllegalStateException` で fail-fast します。

## 可観測性

アプリケーションに Micrometer の `MeterRegistry` があれば、starter は構造化出力関連のカウンターを自動公開します。特定の監視バックエンドに縛られず、Spring Boot がサポートする任意の Micrometer registry で収集できます。

guard の挙動はそのままに、組み込み Micrometer リスナーだけ無効化したい場合は `spring.ai.structured-output.guard.metrics.enabled=false` を設定してください。

| Metric | Tags | Meaning |
|---|---|---|
| `spring.ai.structured.output.guard.calls` | `result=success|repaired_success|failure` | 最終結果ごとの完了呼び出し総数 |
| `spring.ai.structured.output.guard.repair.attempts` | なし | 解析失敗後にローカル repair フローへ入った回数 |
| `spring.ai.structured.output.guard.repair.success` | なし | repair 後の内容が正常に解析できた回数 |
| `spring.ai.structured.output.guard.retries` | `error_type=structured_output|other` | 実際にスケジュールされた再試行回数 |
| `spring.ai.structured.output.guard.failures` | `error_type=structured_output|other` | guard 処理後の最終失敗回数 |

`repair.attempts` はトップレベルリクエスト数ではなく、repair pass 数を数えます。同じリクエストが 2 回解析に失敗し、最終結果に至るまで 2 回 repair に入れば、このカウンターは `2` 増えます。

Spring starter ではなく `core` を直接使う場合でも、自前の `StructuredOutputExecutionListener` を `StructuredOutputExecutor` に渡して、同じライフサイクルイベントを既存の可観測基盤へ流せます。

## 🧱 プロジェクト構成

- `core`
  Spring コンテナに依存しない再試行、修復、解析オーケストレーション。
- `starter`
  Spring Boot 自動構成と、そのまま使える統合エントリポイント。
- `example`
  実行可能なデモアプリ。

## 📌 互換性

2026 年 4 月 28 日時点で、このリポジトリで実際に検証済みの組み合わせは次のとおりです。

| Spring Boot | Spring AI | 状態 | 検証方法 |
|---|---|---|---|
| `4.0.1` | `2.0.0-M5` | 検証済み | デフォルトの `./gradlew test` パスで `core`、`starter`、`example` を検証 |
| `3.5.11` | `1.1.4` | 検証済み | `-PspringBootVersion=3.5.11 -PspringAiVersion=1.1.4` 付き互換性マトリクス実行 |

2026 年 4 月 28 日時点で公開されている Spring AI のバージョンラインは次のとおりです。

- Stable `1.0.5` と `1.1.4`
- Preview `2.0.0-M5`

リポジトリのデフォルトビルドは現在 `Spring Boot 4.0.1 + Spring AI 2.0.0-M5` を対象にしています。

そのほかの Spring AI ラインは CI の互換性マトリクスではまだ未カバーなので、明示的に追加・検証されるまでは未検証として扱ってください。

## 🤝 コントリビュート

Issue と Pull Request を歓迎します。

- リリースノート: [CHANGELOG.md](./CHANGELOG.md)
- 参加方法: [CONTRIBUTING.md](./CONTRIBUTING.md)
