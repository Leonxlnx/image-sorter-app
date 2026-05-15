# Contributing to PhotoSwipe

Thanks for taking the time to contribute. This document is intentionally short — PhotoSwipe is a small project and we want the bar for contributing to stay low.

## Ground rules

- Be respectful. We follow the [Contributor Covenant](CODE_OF_CONDUCT.md).
- Prefer small, focused pull requests over sprawling ones.
- Open an issue first if you are unsure whether a change is in scope.
- Do not introduce analytics, ads, network calls, or any kind of tracking. Privacy is a core feature.

## Development setup

1. Install JDK 17 and the Android SDK (platform 34, build-tools 34).
2. Clone the repo and open it in Android Studio Iguana / Jellyfish or newer, or build from the command line:
   ```bash
   ./gradlew :app:assembleDebug
   ```
3. Run the lint checks before opening a PR:
   ```bash
   ./gradlew :app:lintDebug
   ```

## Coding standards

- Kotlin, idiomatic, no Java additions.
- Jetpack Compose with Material 3. Avoid pulling in unmaintained Compose-related libraries.
- Stay on the existing dependency set unless there is a strong reason to add something.
- Keep ViewModels free of Android framework imports beyond `Application` / `Context` and `ViewModel`.
- Persist new user preferences via `SettingsRepository` (DataStore), not `SharedPreferences`.
- Public-facing strings live in `app/src/main/res/values/strings.xml` and must be grammatically correct English. Use `stringResource(...)` in Compose, never hard-coded text.
- Match the existing naming and section ordering in `SettingsScreen.kt` when adding new settings.

## Commit / PR conventions

- Branch from `main`. Branch names: `devin/<timestamp>-short-topic`, `feature/short-topic` or similar.
- Commit messages: short imperative subject, optional body. Conventional Commits are welcome but not required.
- Open the PR against `main` and fill in the PR template — describe **what** changed and **why**.
- Mention any new permission, behavior change, or storage write in the PR description.
- Add an `Unreleased` entry to [CHANGELOG.md](CHANGELOG.md) describing the change.

## What we are unlikely to accept

- Adding `android.permission.INTERNET` or networking dependencies.
- Background services or scheduled jobs.
- UI rewrites in XML / View system.
- Telemetry, crash reporters, error trackers.
- Changes that break the 4-way swipe semantics described in the README.

## Questions

Open a [Discussion](https://github.com/Leonxlnx/image-sorter-app/discussions) or a feature-request issue. We respond when we can.
