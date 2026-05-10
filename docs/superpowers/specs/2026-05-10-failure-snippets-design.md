# Failure Content Snippets Design

**Issue:** #13 — 支持可选的失败内容片段
**Date:** 2026-05-10

## Goal

Support optional, controlled, default-off exposure of raw/repaired model content in `StructuredOutputFailureContext` for production troubleshooting.

## Scope

- Failure snippet capture in core module
- Configuration in core options and starter properties
- Tests for all paths
- README documentation with security boundary

Out of scope: auto-redaction, audit logging, full payload persistence.

---

## 1. New Record: `FailureSnippets`

**File:** `core/src/main/java/io/github/kiyragjx/saiguard/core/FailureSnippets.java`

```java
public record FailureSnippets(
    String lastRawContentSnippet,
    String lastRepairedContentSnippet
) {
    public static final FailureSnippets EMPTY = new FailureSnippets(null, null);
}
```

- Nullable fields: null means "not captured"
- No normalization — truncation applied at capture time by the tracker

## 2. Modified: `StructuredOutputFailureContext`

Add `snippets` field:

```java
public record StructuredOutputFailureContext(
    int attemptCount,
    boolean repairAttempted,
    boolean repairSucceeded,
    String errorType,
    FailureSnippets snippets  // nullable
)
```

- `EMPTY` sentinel uses `null` snippets
- `isEmpty()` unchanged — only checks original 4 fields
- Backward-compatible: existing 4-arg construction gets `null` snippets

## 3. Modified: `StructuredOutputOptions`

Two new fields:

| Field | Type | Default | Constraint |
|---|---|---|---|
| `failureSnippetsEnabled` | `boolean` | `false` | — |
| `failureSnippetsMaxLength` | `int` | `500` | min 64 |

Builder gets corresponding setters.

## 4. Modified: `ExecutionFailureTracker`

New fields:
- `lastRawContent` (String)
- `lastRepairedContent` (String)

New methods:
- `recordRawContent(String rawContent)` — called on every attempt
- `recordRepairedContent(String repairedContent)` — called after repair produces different content

Modified `toContext(boolean snippetsEnabled, int snippetsMaxLength)`:
- When disabled: returns context with `null` snippets
- When enabled: truncates captured content to `snippetsMaxLength` and builds `FailureSnippets`

Truncation: `content.length() > maxLength ? content.substring(0, maxLength) : content`

## 5. Modified: `StructuredOutputExecutor`

### `parseWithRepair()`

- Call `failureTracker.recordRawContent(rawContent)` at the top (always, before parse attempt)
- Call `failureTracker.recordRepairedContent(repaired)` after repair produces different content, before parser attempt

### `toContext()` call sites

- `failureTracker.toContext(options.failureSnippetsEnabled(), options.failureSnippetsMaxLength())`

## 6. Starter Properties

Nested class in `StructuredOutputGuardProperties`:

```java
public static class FailureSnippets {
    private boolean enabled = false;
    private int maxLength = 500;
    // getters/setters
}
private FailureSnippets failureSnippets = new FailureSnippets();
```

Properties:
- `spring.ai.structured-output.guard.failure-snippets.enabled=false`
- `spring.ai.structured-output.guard.failure-snippets.max-length=500`

Mapped to `StructuredOutputOptions` in auto-configuration.

## 7. Tests

### Core (`StructuredOutputExecutorTest`)

1. **Default (disabled):** `failureContext.snippets()` is null
2. **Enabled, raw content captured:** `lastRawContentSnippet` contains truncated raw content
3. **Enabled, repair path:** `lastRepairedContentSnippet` contains truncated repaired content
4. **Max-length enforcement:** content exceeding limit is truncated
5. **No repair attempted:** `lastRepairedContentSnippet` is null
6. **Core builder:** options builder sets snippet fields correctly

### Starter (`StructuredOutputGuardAutoConfigurationTest`)

7. **Properties binding:** failure-snippets properties map to options
8. **Default disabled:** auto-configured executor produces null snippets

### Metadata (`ConfigurationMetadataConsistencyTest`)

9. New properties appear in metadata and README tables

## 8. README

Add section documenting:
- `spring.ai.structured-output.guard.failure-snippets.enabled`
- `spring.ai.structured-output.guard.failure-snippets.max-length`
- Security boundary: may contain sensitive content, default off, caller assesses compliance risk

---

## Acceptance Criteria Mapping

| Criterion | Implementation |
|---|---|
| Default: no raw/repaired snippets in exception | `failureSnippetsEnabled` defaults to `false`, `snippets` is `null` |
| Enabled: last raw content snippet available | `recordRawContent()` + `buildSnippets()` |
| Enabled: last repaired content snippet available | `recordRepairedContent()` + `buildSnippets()` |
| Snippet length strictly limited | Truncation in `buildSnippets()` |
| README documents security boundary | New README section |
