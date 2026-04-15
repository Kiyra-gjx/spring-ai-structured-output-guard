# Release Plan

This repository is not ready for a `0.1.0` stable release yet.

The recommended first public release is:

```text
0.1.0-beta.1
```

## Why Beta First

- the project currently targets Spring Boot `4.0.1` and Spring AI `2.0.0-M1`
- Spring AI integration lives on preview-era dependencies
- only the `core` module has tests today
- `starter` and `example` still lack release-facing verification
- the artifact is not published yet, so installability has not been proven end-to-end

## Release Checklist

- publishable Gradle configuration now exists for the `core` and `starter` modules
- POM metadata is configured in the Gradle publications
- add at least one `starter` auto-configuration test
- add at least one end-to-end integration or smoke test for the example flow
- verify the build with Java 21 in local development and CI
- publish `0.1.0-beta.1`
- create a Git tag and GitHub Release with install instructions
- update all README installation examples from "planned" to the real published coordinates

## Publishing Commands

For snapshots:

```bash
./gradlew publishToSonatypeCentral -PreleaseVersion=0.1.0-SNAPSHOT
```

For a beta or release candidate:

```bash
./gradlew publishReleaseToCentralPortal -PreleaseVersion=0.1.0-beta.1
```

## Required Environment Variables

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_SIGNING_KEY`
- `MAVEN_SIGNING_PASSWORD`

Optional:

- `CENTRAL_PUBLISHING_TYPE`
  Defaults to `user_managed`. Set it to `automatic` if you want the Central Portal to publish automatically after upload.

## Stable Release Gate

Consider `0.1.0` only after:

- the beta coordinates have been installed successfully by real users
- the API names feel stable enough to keep
- Spring AI compatibility is clearer
- at least the starter path has basic verification coverage
