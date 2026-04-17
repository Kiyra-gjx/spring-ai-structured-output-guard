# Release Plan

`0.1.0` has been released.

Published releases:

```text
0.1.0
0.1.0-beta.1
```

## Current Status

- `0.1.0` is published on Maven Central and has a GitHub Release + tag
- the project currently targets Spring Boot `4.0.1` and Spring AI `2.0.0-M1`
- the `starter` path now has auto-configuration coverage
- the `example` flow now has a controller-level smoke test
- external installability has already been verified through a standalone Maven demo

## Stable Release Checklist

Completed:

- publishable Gradle configuration now exists for the `core` and `starter` modules
- POM metadata is configured in the Gradle publications
- add at least one `starter` auto-configuration test
- add at least one end-to-end integration or smoke test for the example flow
- verify the build with Java 21 in local development and CI
- publish `0.1.0-beta.1`
- create a Git tag and GitHub Release with install instructions
- update all README installation examples to the published beta coordinates

Next follow-up items:

- keep the public API stable across `0.1.x`
- monitor compatibility with newer Spring AI releases
- document the next release flow when `0.1.1` or `0.2.0` is planned

## Publishing Commands

For snapshots:

```bash
./gradlew publishToSonatypeCentral -PreleaseVersion=0.1.0-SNAPSHOT
```

For a beta or release candidate:

```bash
./gradlew publishReleaseToCentralPortal -PreleaseVersion=0.1.0-beta.1
```

For a stable release:

```bash
./gradlew publishReleaseToCentralPortal -PreleaseVersion=0.1.0
```

On Windows PowerShell, prefer one of these forms to avoid argument parsing issues with dotted version numbers:

```powershell
.\gradlew.bat publishReleaseToCentralPortal "-PreleaseVersion=0.1.1"
```

or:

```powershell
$env:ORG_GRADLE_PROJECT_releaseVersion = "0.1.1"
.\gradlew.bat publishReleaseToCentralPortal
```

## Interactive Options

### IntelliJ IDEA

You do not have to remember the full command if you publish from IDEA.

1. Open the Gradle tool window.
2. Find the root project task `publishing > publishReleaseToCentralPortal`.
3. Run it from the UI.
4. If you need to publish a specific version, edit the run configuration for that task and add:

```text
-PreleaseVersion=0.1.1
```

If the parameter is not passed through correctly in PowerShell, set `ORG_GRADLE_PROJECT_releaseVersion` in the run configuration environment variables instead.

Environment variables still need to be present in the run configuration:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_SIGNING_KEY`
- `MAVEN_SIGNING_PASSWORD`

### GitHub Actions

A manual workflow is available at:

```text
Actions > Manual Release > Run workflow
```

Before using it, add these repository secrets in GitHub:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_SIGNING_KEY`
- `MAVEN_SIGNING_PASSWORD`

The workflow publishes to Maven Central. Git tag creation and the GitHub Release page can still be done afterwards.

## Required Environment Variables

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_SIGNING_KEY`
- `MAVEN_SIGNING_PASSWORD`

Optional:

- `CENTRAL_PUBLISHING_TYPE`
  Defaults to `user_managed`. Set it to `automatic` if you want the Central Portal to publish automatically after upload.

## Stable Release Gate

The `0.1.0` release baseline is:

- the published coordinates work in a standalone external project
- the full test suite is green with Java 21
- the repository example application runs successfully
