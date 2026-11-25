# AGENTS.md

Guidance for engineers and automation working in this repository.

## Environment

- JDK 8 (`JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64`). AGP 4.2.2 does not run on JDK 17+.
- `ANDROID_SDK_ROOT` set, or `sdk.dir` in `local.properties` (not committed).
- SDK packages: `platforms;android-30`, `build-tools;30.0.3`, `platform-tools`, `emulator`,
  `system-images;android-30;google_apis;x86_64`.
- Maven Central is reached through the mirror in `build.gradle`; Robolectric's runtime jars use the
  same mirror (`robolectric.dependency.repo.url` in `app/build.gradle`). Keep both if you add repos.

## Commands

| Task | Command |
|---|---|
| Build offline APK | `./gradlew assembleMockDebug` |
| Build live APK | `./gradlew assembleLiveDebug` |
| Unit + Robolectric tests | `./gradlew testMockDebugUnitTest` |
| Single test class | `./gradlew testMockDebugUnitTest --tests 'com.northbridge.mobile.util.MoneyTest'` |
| Coverage report | `./gradlew jacocoTestReport` -> `app/build/reports/jacoco/jacocoTestReport/` |
| Lint | `./gradlew lintMockDebug` |
| Install + launch | `./gradlew installMockDebug && adb shell am start -n com.northbridge.mobile.mock/com.northbridge.mobile.ui.login.LoginActivity` |
| Screenshot | `adb exec-out screencap -p > screen.png` |

Seeded login for the `mock` flavour: `dana.whitfield` / `Welcome1!`.

## Conventions

- Package root `com.northbridge.mobile`. Screens live in `ui/<feature>/`, one Activity per screen.
- New production code: Java 8 unless it is a pure utility or lives in `src/mock` (Kotlin). Do not
  use Java 9+ APIs or `java.time` (minSdk 23); use `BankDates`.
- Money is `BigDecimal` via `Money.kt`; never `double`. Send amounts with `Money.toWire`.
- Mask account numbers with `AccountNumbers.mask` anywhere they are shown or logged.
- API shapes follow the core platform `docs/API.md` exactly. When adding an endpoint, add it to
  the Retrofit interface, the DTO in `model/`, and a route in `MockBackendInterceptor` so the
  `mock` flavour keeps working.
- User-facing text goes in `res/values/strings.xml`.
- Tests: JUnit 4 under `app/src/test`, Robolectric for anything touching Android classes
  (`@Config(sdk = 28)`). Run against the `mockDebug` variant.
- Dependency upgrades: one library family per change; note the reason in the commit message.
