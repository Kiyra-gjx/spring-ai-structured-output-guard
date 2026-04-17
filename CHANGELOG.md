# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project follows Semantic Versioning once the first release is published.

## [Unreleased]

### Added

- starter auto-configuration tests covering default beans, property binding, and bean override behavior
- MVC smoke test for the example controller using repairable malformed JSON
- public publishing configuration for `core` and `starter`
- Sonatype Central compatible publication tasks and signing hooks
- issue templates, contribution guide, and release-oriented repository metadata

## [0.1.0] - 2026-04-17

### Added

- starter auto-configuration tests covering default beans, property binding, and bean override behavior
- MVC smoke test for the example controller using repairable malformed JSON
- manual GitHub Actions workflow for release publishing
- release documentation updates for PowerShell, IntelliJ IDEA, and GitHub Actions flows

## [0.1.0-beta.1] - 2026-04-15

### Added

- `core` module with retry orchestration and JSON repair
- `starter` module with Spring Boot auto-configuration
- `SpringAiStructuredOutputGuard` integration for Spring AI `ChatClient`
- lightweight JSON cleanup for code fences, JSON body extraction, trailing commas, smart quotes, and control characters
- unit tests for repair and retry behavior
- runnable Spring Boot example application
- GitHub Actions CI workflow
- public publishing configuration for `core` and `starter`
- Sonatype Central compatible publication tasks and signing hooks
- issue templates, contribution guide, and release-oriented repository metadata
