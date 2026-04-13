# Spring AI Structured Output Guard

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M1-6DB33F)](https://spring.io/projects/spring-ai)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

Spring AI 向けの構造化出力ガードです。

`spring-ai-structured-output-guard` は、Spring AI の構造化出力呼び出しを安全に包むための小さなライブラリです。対象を絞ったリトライ、軽量な JSON 修復、そして Spring Boot Starter を提供し、壊れやすい `try/catch + retry + cleanup` を毎回書かなくて済むようにします。

## 解決したい問題

Spring AI の `BeanOutputConverter` は、モデルが正しい JSON を返す限り非常に便利です。

しかし実運用では、次のような応答がよく返ってきます。

````text
```json
{
  "name": "Alice",
  "skills": ["Java", "Spring",],
}
```
````

または:

```text
Here is the result you asked for:
{"name":"Alice","summary":"line1
line2"}
```

どちらも「ほぼ」正しい JSON ですが、解析には失敗します。

## 主な機能

- 構造化出力の解析エラーらしい場合にだけリトライ
- Markdown code fence と余分な前後文を除去
- ノイズ混じりの応答から JSON 本体を抽出
- 末尾カンマを除去
- スマートクォートを正規化
- JSON 文字列内の制御文字をエスケープ
- 呼び出し側コードを小さく保つ
- Spring Boot 自動設定を提供

## インストール

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

## クイックスタート

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

## 設定

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

## 現在の修復対象

- UTF-8 BOM の除去
- Markdown code fence の除去
- 最初の JSON object / array の抽出
- スマートクォートの正規化
- `}` / `]` 前の末尾カンマ削除
- JSON 文字列内の改行や制御文字のエスケープ

## 例アプリ

`example` モジュールは次のエンドポイントを提供します。

```text
GET /demo/movie-review?movie=Interstellar
```

起動:

```bash
./gradlew :example:bootRun
```

## ローカル開発

```bash
./gradlew test
./gradlew :example:bootRun
```

## 互換性

現在のスキャフォールドは以下の組み合わせで構成されています。

- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`
- Java `21`

公開リリース前には、Spring AI の安定版ラインへ移行する想定です。

## ロードマップ

- custom repair strategy
- Micrometer metrics
- ストリーミング集約後の構造化出力サポート
- より豊富な統合テスト

## コントリビュート

Issue と PR を歓迎します。詳細は [CONTRIBUTING.md](./CONTRIBUTING.md) を参照してください。

