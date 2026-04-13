# Contributing

Thanks for considering a contribution.

## Development Setup

Requirements:

- Java 21
- Gradle wrapper

Run locally:

```bash
./gradlew test
./gradlew :example:bootRun
```

## Contribution Scope

Good contributions for this repository:

- bug fixes in JSON repair and retry behavior
- compatibility improvements for Spring AI and Spring Boot
- better tests around malformed structured output
- metrics, logging, and observability improvements
- better examples and documentation

Please avoid large unrelated refactors in a single pull request.

## Pull Request Guidelines

- keep changes focused
- add or update tests for behavior changes
- update `README.md` or `CHANGELOG.md` when user-facing behavior changes
- prefer small, reviewable pull requests

## Reporting Bugs

When opening an issue, include:

- Spring Boot version
- Spring AI version
- Java version
- minimal reproducible code
- raw model response if possible
- expected vs actual behavior

