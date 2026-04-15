# Spring AI Structured Output Guard

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M1-6DB33F)](https://spring.io/projects/spring-ai)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

Spring AI の構造化出力を本番環境でも信頼できるものにします。

`spring-ai-structured-output-guard` は、Spring AI の構造化出力呼び出しを包み込み、対象を絞ったリトライと保守的な JSON 修復を提供します。壊れた応答のたびに、各サービスで `try/catch + retry + cleanup` を繰り返し書かなくて済むようにするためのライブラリです。

## なぜ必要か

Spring AI は `BeanOutputConverter` を提供しており、モデルがクリーンな JSON を返すなら十分に便利です。

しかし実際の本番トラフィックはもっと汚く、構造化出力は次のような内容で簡単にパース失敗します。

- Markdown code fence で囲まれた JSON
- `}` や `]` の直前にある末尾カンマ
- JSON 本体の前後に混ざる説明文や余分なテキスト
- JSON 文字列内に含まれる生の改行や制御文字
- 本来は 1 回の対象を絞ったリトライで済むのに、独自の復旧処理全体を書かされるようなパース失敗

これらの応答は「ほぼ」正しい JSON ですが、ほぼ正しいだけでは解析に失敗します。

### 典型的な壊れた応答

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

## 得られるもの

- 構造化出力の解析エラーらしい場合にだけ、対象を絞ったリトライを実行
- よくある低リスクな問題に対する軽量な JSON 修復
- ノイズ混じりの応答から実際の JSON 本体を抽出
- 末尾カンマの除去とスマートクォートの正規化
- JSON 文字列内の生の制御文字のエスケープ
- 簡潔で型安全な呼び出しコード
- Spring AI プロジェクトにそのまま組み込める Spring Boot Starter

## 30 秒で導入

**Guard なし**

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

**Guard あり**

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

## インストール

現在、この Starter はまだ公開アーティファクトレジストリには公開されていません。

最初の公開リリース予定:

```text
0.1.0-beta.1
```

予定している依存座標:

### Gradle

```groovy
implementation "io.github.kiyragjx:spring-ai-structured-output-guard-starter:0.1.0-beta.1"
```

### Maven

```xml
<dependency>
  <groupId>io.github.kiyragjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0-beta.1</version>
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
