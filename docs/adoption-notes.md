# Choosing Between BeanOutputConverter, StructuredOutputValidationAdvisor, and This Guard

`spring-ai-structured-output-guard` is not trying to replace Spring AI structured output. It fills one narrow gap: malformed JSON that is still close enough to recover.

## Start With Native Structured Output When You Can

If your model provider supports native structured output or JSON schema mode, that should still be the first choice. Spring AI documents `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT` as the more reliable option because the schema goes directly to the model API instead of being appended as prompt text.

This guard is mainly for teams that are still using prompt-based structured output flows and keep running into small, recurring parse failures in real traffic.

## Short Comparison

| Tool | Best fit | Local malformed JSON cleanup | Retry behavior | Validation behavior | Notes |
|---|---|---|---|---|---|
| `BeanOutputConverter` | The model usually returns valid JSON | No | No | Converts model output into a typed object using generated schema instructions | This is the normal baseline |
| `StructuredOutputValidationAdvisor` | You want repeat attempts plus schema validation for structured JSON responses | No cleanup layer | Repeats the call when validation fails | Validates structured JSON output against the expected schema | Good for schema mismatch, not for cleaning code fences or trailing commas |
| `spring-ai-structured-output-guard` | Your responses are often almost valid JSON, but still fail parsing | Yes, but only for a small low-risk set | Retries only when the error still looks like structured-output parsing | No separate schema-validation layer beyond the converter and parser you already use | Wraps the existing `BeanOutputConverter` flow instead of replacing it |

## What This Guard Actually Adds

The starter keeps the standard Spring AI pattern:

1. append `BeanOutputConverter.getFormat()` to the prompt
2. call the `ChatClient`
3. parse the response with `BeanOutputConverter`

The extra behavior is intentionally small:

1. if parsing fails, try lightweight local repair
2. if repair still does not get the payload through parsing, classify the error
3. retry only when it still looks like a structured-output parsing problem
4. throw `StructuredOutputException` when the attempts are exhausted

That means this library is a recovery layer around prompt-based structured output. It is not a general validation framework and not a generic JSON repair engine.

## Malformed JSON Sample Matrix

The matrix below reflects the current implementation in `JsonRepairer` and the retry path in `StructuredOutputExecutor`.

| Response shape | Example | Local repair | Falls back to targeted retry | Notes |
|---|---|---|---|---|
| Markdown code fence | ```` ```json {"name":"Alice"} ``` ```` | Yes | No, if repair succeeds | Fence is stripped before parsing |
| Leading or trailing prose | `Here is the result: {"name":"Alice"}` | Yes | No, if repair succeeds | First JSON object or array is extracted |
| Trailing commas | `{"tags":["java",],}` | Yes | No, if repair succeeds | Only commas before `}` or `]` are removed |
| Smart quotes | Curly-quoted JSON such as `“name”: “Alice”` inside an object | Yes | No, if repair succeeds | Curly quotes are normalized |
| Raw newlines or tabs in JSON strings | `{"summary":"line1<newline>line2"}` | Yes | No, if repair succeeds | Control characters inside strings are escaped |
| Single-quoted pseudo JSON | `{'name':'Alice'}` | No | Yes | The guard does not rewrite this locally |
| JSON comments | `{"name":"Alice" // comment}` | No | Yes | Comment stripping is intentionally out of scope |
| Missing closing brace or bracket | `{"name":"Alice"` | No | Yes | The guard does not guess missing structure |
| Valid JSON but wrong shape | `{"score":"high"}` when the target expects a number | No | Sometimes | Use schema validation or domain validation for this class of problem |

## Practical Rule Of Thumb

- Native structured output available: use that first.
- Valid JSON most of the time: use plain `BeanOutputConverter`.
- Need schema validation and repeated calls for structured JSON: use `StructuredOutputValidationAdvisor`.
- Need to absorb code fences, wrapper prose, trailing commas, or control-character issues without copying retry logic into every service: use this guard.

## Test Coverage For These Claims

The supported repair cases and retry behavior are covered by the current unit tests:

- [`core/src/test/java/io/github/kiyragjx/saiguard/core/JsonRepairerTest.java`](../core/src/test/java/io/github/kiyragjx/saiguard/core/JsonRepairerTest.java)
- [`core/src/test/java/io/github/kiyragjx/saiguard/core/StructuredOutputExecutorTest.java`](../core/src/test/java/io/github/kiyragjx/saiguard/core/StructuredOutputExecutorTest.java)
