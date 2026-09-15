# Repository Guidelines

## Project Structure & Module Organization

Bike Companion is a single-module Android application built with Kotlin, Jetpack Compose, Hilt, and Room. Production code lives under `app/src/main/java/com/you/bikecompanion/`. Keep UI screens and ViewModels in `ui/`, persistence code in `data/`, dependency injection in `di/`, and integrations in their existing packages (`ai/`, `healthconnect/`, `location/`, and `notifications/`). Android resources belong in `app/src/main/res/`; put user-visible text in `values/strings.xml`. Local unit tests mirror production packages under `app/src/test/`, while device and database migration tests live under `app/src/androidTest/`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper with JDK 17 and Android SDK 36:

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew installDebug` installs it on a connected emulator or device.
- `./gradlew testDebugUnitTest` runs JVM unit tests.
- `./gradlew connectedDebugAndroidTest` runs instrumented and Compose tests on a connected device.
- `./gradlew lintDebug` runs Android static analysis.

## Coding Style & Naming Conventions

Follow Kotlin's standard four-space indentation and the formatting already present in nearby files. Use `PascalCase` for classes, Compose functions, and files; use `camelCase` for functions and properties; use `UPPER_SNAKE_CASE` for constants. Name UI state types `<Feature>UiState` and ViewModels `<Feature>ViewModel`. Keep composables focused, expose immutable `StateFlow` values, inject dependencies through constructors, and place database access behind repositories. Prefer simple, readable changes over new abstractions. Never hardcode display text in Kotlin.

## Testing Guidelines

Write the failing test before the implementation. Use JUnit 4, MockK, and `kotlinx-coroutines-test` for local tests; use AndroidX JUnit, Espresso, and Compose UI testing for device tests. Name test classes after the subject and test methods by behavior, for example `saveBike_emptyName_doesNotCallRepository`. Add migration coverage whenever the Room schema changes. There is no configured coverage threshold; cover new behavior and regressions directly.

## Commit & Pull Request Guidelines

Recent commits use imperative, outcome-focused subjects such as `Add Gemini API client...` and `Enhance component management...`. Keep commits focused. Branch from a non-default branch using `feature/<issue>-<summary>` or `bug/<issue>-<summary>`; never push directly to `main`. Pull requests should explain the change, list test commands and results, link the issue, and include screenshots for UI changes. Call out database migrations, permissions, or privacy impacts explicitly.

## Security & Configuration

Do not commit API keys, `local.properties`, `.env` files, or `secrets.properties`. Treat location and Health Connect data according to `PRIVACY.md` and keep secrets in the existing secure-preferences path.


review /code/agents.md for more info on how we will be doing work: # Global Working Agreements
- Prefer boring, readable, predictable code over clever code. Reuse established project patterns. Do not introduce a new abstraction unless the existing code clearly requires one to implement the requested behavior cleanly.
- in an attempt to keep token usage low , codex will be passed a prompt for implementation of a feature a bug fix or a testing plan. codex is to implement it, then provide the user a ready-to-run local OpenCode prompt for the relevant test suite instead of invoking OpenCode itself.

- Keep it simple, stupid (KISS): prefer the simplest solution that clearly meets the requirement.
- Write clean, readable, unsurprising, and maintainable code.
- Choose names that are pronounceable, descriptive, and consistent with the surrounding code.
- Write as if the person maintaining the code is a homicidal maniac who knows your address; make the code so clear and considerate that they will like you instead.
- Favor the Single Responsibility Principle: each function, class, and module should have one clear reason to change.
- Keep changes focused, avoid cleverness, and leave the code easier to understand than you found it.
- write the test then make it pass.

## Branch Naming

- Create a new branch for each change, using a short type, issue number, and kebab-case summary.
- Use `feature/33-add-the-thing` for new functionality and `bug/33-fix-the-thing` for fixes. This corresponds to the human-readable style “feature 33 - add the thing” or “bug 33 - fix the thing.”

## Protected Default Branches

- Do not bypass this policy through Git, GitHub CLI, an API, a connector, or another tool.
- Never push directly to `origin/main`.
- When work starts from a protected default branch, create or use a non-default branch and leave merging to a pull request or the user’s manual workflow
- Other agents may be working at the same time as you, use git best practices to avoid interference.
- Before making any repository change, including code, tests, configuration, migrations, documentation, or generated artifacts, check the current branch and working-tree status.
## SAVE TOKENS
- codex is not for discussion if a long discussion needs to happen let the user know, and provide a chatgpt prompt. codex can write the code and do small back and forths, but when it make sense move to chatgpt.

