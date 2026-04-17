# Spring AI Structured Output Guard 🛡️

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

**モデルのちょっとした「書き崩れ」で、構造化出力全体を落とさないためのガードです。**

Spring AI で `BeanOutputConverter` を本番利用していると、モデル全体の精度が安定していても、Markdown の code fence 付き JSON、末尾カンマ付き JSON、説明文混じりの JSON がたまに返ってきます。サービスコード側で毎回クリーンアップや再試行や例外ラップを書くより、その役割を guard レイヤーに寄せた方が扱いやすくなります。

## 🚀 何を解決するか

モデルの応答が「ほぼ JSON」だがそのままでは扱えないとき、Guard はまずローカル修復を試し、その後で定向再試行が必要かどうかを判断します。

- コードフェンス
  Markdown code fence を剥がしてからパースします。
- ノイズ混在
  説明文の中から最初の JSON object / array を抜き出します。
- 軽い構文崩れ
  末尾カンマ、smart quotes、JSON 文字列内の生制御文字を修正します。
- 解析失敗
  それでも失敗する場合でも、構造化出力の解析エラーらしいときだけ再試行します。

詳しい比較と malformed JSON のサンプル行列は [docs/adoption-notes.md](./docs/adoption-notes.md) を参照してください。

## 🛠️ クイックスタート

### 1. 依存関係を追加

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2. `ChatClient` の相棒として呼び出す

Guard は「モデル呼び出し -> 解析 -> 必要なら修復 -> 必要なら再試行」までをまとめて扱うので、`converter.convert()` や例外の分類を毎回自分で書く必要はありません。

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
| Native structured output | プロバイダが公式に対応していて、まずそこを使いたい |
| 素の `BeanOutputConverter` | モデルが大半のケースで正しい JSON を返し、回復レイヤーが不要 |
| `StructuredOutputValidationAdvisor` | schema 検証や再試行は欲しいが、JSON 清掃は本質ではない |
| Spring AI Guard | code fence、末尾カンマ、説明文混在、制御文字のような「汚れているがまだ救える JSON」が問題 |

3 つの違いを詳しく見たい場合は [docs/adoption-notes.md](./docs/adoption-notes.md) を参照してください。

## ⚙️ 設定

設定プレフィックスは `spring.ai.structured-output.guard` です。

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
| `spring.ai.structured-output.guard.max-attempts` | `2` | 最初の呼び出しを含む総試行回数 |
| `spring.ai.structured-output.guard.enable-repair` | `true` | 再試行前に軽量 JSON 修復を有効にする |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | 前回の解析エラー要約を再試行プロンプトに含める |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | 再試行プロンプトに含めるエラーメッセージ長を制限する |

## 🔧 修復ポリシー

修復レイヤーは意図的に保守的です。低リスクで正規化しやすいフォーマットノイズだけを扱います。

- UTF-8 BOM を除去する
- Markdown code fence を剥がす
- 最初の JSON object / array を抽出する
- smart quotes を正規化する
- `}` / `]` の前にある末尾カンマを除去する
- JSON 文字列内の生 `\n`、`\r`、`\t` と他の制御文字をエスケープする

次のことはしません。

- 欠けた波括弧や角括弧を推測する
- 単引用符ベースの疑似 JSON を標準 JSON に書き換える
- JSON コメントを削除する
- すでに意味的に壊れている payload を修正する

こうしたケースは定向再試行に回るか、最終的に `StructuredOutputException` を返します。

## 🧱 プロジェクト構成

- `core`
  Spring コンテナに依存しない再試行、修復、解析オーケストレーション。
- `starter`
  Spring Boot 自動構成と、そのまま使える統合入口。
- `example`
  実行できるデモアプリ。

## 📌 互換性

このリポジトリで実際に検証している基線:

- Java `21`
- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`

2026 年 4 月 17 日時点の Spring AI 公開バージョンライン:

- Stable `1.0.5` と `1.1.4`
- Preview `2.0.0-M4`

`0.1.0` は上の基線で確認済みという意味であり、現在公開されているすべての Spring AI ラインへの包括的な互換性主張ではありません。

## 🤝 コントリビュート

Issue と Pull Request を歓迎します。

- リリースノート: [CHANGELOG.md](./CHANGELOG.md)
- 参加方法: [CONTRIBUTING.md](./CONTRIBUTING.md)
