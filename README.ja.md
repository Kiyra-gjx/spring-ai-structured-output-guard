# Spring AI Structured Output Guard

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M1-6DB33F)](https://spring.io/projects/spring-ai)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

Spring AI の構造化出力呼び出し向けの小さなガードレイヤーです。

`spring-ai-structured-output-guard` は、Spring AI の構造化出力フローに薄い保護を追加します。失敗がパース問題に見える場合だけリトライし、諦める前に軽い JSON 整形を試します。

公開済みの Maven Central 座標を使った独立 Maven プロジェクトで、外部からの組み込みも検証済みです。OpenAI 互換の Chat Completions API でも動作を確認しています。

## なぜ必要か

Spring AI には `BeanOutputConverter` があり、モデルが正しい JSON を返すならそれで十分です。

このライブラリが対象にしているのは、JSON にかなり近いが、そのままではパースできない応答です。よくあるのは次のようなものです。

- Markdown code fence で囲まれた JSON
- `}` や `]` の直前にある末尾カンマ
- JSON 本体の前後に混ざる説明文や余分なテキスト
- JSON 文字列内に含まれる生の改行や制御文字
- 本来は 1 回の対象を絞ったリトライで済むのに、独自の復旧処理全体を書かされるようなパース失敗

この種の問題は、アプリケーションコード側で毎回同じリトライや後処理を書く原因になりがちです。

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

## できること

- 構造化出力の解析エラーらしい場合だけリトライ
- よくある低リスクな問題に対する軽量な JSON 修復
- ノイズ混じりの応答から JSON 本体を抽出
- 末尾カンマの除去とスマートクォートの正規化
- JSON 文字列内の制御文字をエスケープ
- 呼び出しコードを小さく保ちながら型安全に利用
- Spring AI 向けの Spring Boot Starter を提供

## 基本例

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

この Starter は Maven Central に公開済みです。

現在の公開バージョン:

```text
0.1.0-beta.1
```

安定版 `0.1.0` は現在準備中です。

依存座標:

### Gradle

```groovy
implementation "io.github.kiyra-gjx:spring-ai-structured-output-guard-starter:0.1.0-beta.1"
```

### Maven

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
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

Windows PowerShell では `curl` の代わりに `curl.exe` または `Invoke-RestMethod` を使うと扱いやすいです。

## ローカル開発

```bash
./gradlew test
./gradlew :example:bootRun
```

## 互換性

現在のビルドとテストは次の組み合わせを前提にしています。

- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`
- Java `21`

現在の公開ラインは Spring AI `2.0.0-M1` ベースです。API が落ち着いた後に、より新しい安定版へ寄せるのが自然です。

## リリース状況

安定版 `0.1.0` に向けて残っている主な作業は次のとおりです。

1. 公開 API 名とパッケージ構成の最終確認
2. インストール例の `0.1.0-beta.1` を `0.1.0` に更新
3. `0.1.0` を公開
4. `0.2.x` でメトリクスや拡張ポイントを追加

## ロードマップ

- custom repair strategy
- Micrometer metrics
- ストリーミング集約後の構造化出力サポート
- より豊富な統合テスト

## コントリビュート

Issue と PR を歓迎します。詳細は [CONTRIBUTING.md](./CONTRIBUTING.md) を参照してください。
