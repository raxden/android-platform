# android-convention

A collection of Gradle convention plugins for Android projects, designed to keep a single source of truth for common module configurations.

This approach is based on:
- [Herding Elephants – Square Engineering](https://developer.squareup.com/blog/herding-elephants/)
- [Idiomatic Gradle – Johannes Hüsing](https://github.com/jjohannes/idiomatic-gradle)

Convention plugins avoid duplicated build script setup and messy `subprojects` blocks, without the pitfalls of `buildSrc`. They are **additive** and **composable** — each plugin has a single responsibility, and modules pick only what they need. One-off logic that isn't shared should stay directly in the module's `build.gradle.kts`.

## Structure

```
android-convention/
├── convention/                  # Convention plugins module
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── android-application-conventions.gradle.kts
│       ├── android-library-conventions.gradle.kts
│       ├── android-feature-conventions.gradle.kts
│       ├── android-compose-application-conventions.gradle.kts
│       ├── android-compose-library-conventions.gradle.kts
│       ├── android-compose-feature-conventions.gradle.kts
│       ├── android-project-conventions.gradle.kts
│       └── extension/           # Shared Kotlin helpers for plugins
├── gradle/
│   └── libraries.versions.toml  # Version catalog
└── settings.gradle.kts
```

## Setup

### 1. Add as a Git submodule

From your project's root directory:

```sh
git submodule add git@github.com:raxden/android-convention.git build-logic
```

### 2. Configure `settings.gradle.kts`

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("./gradle/libraries.versions.toml"))
        }
    }
}
```

> `build-logic/settings.gradle.kts` resolves the version catalog automatically: it first looks for `../gradle/libraries.versions.toml` (i.e. the host project's catalog), and falls back to its own `gradle/libraries.versions.toml`. This means **the host project's `gradle/` catalog is the source of truth** and `build-logic` picks it up without any extra configuration.

### 3. Apply `android-project-conventions` in the root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.project.conventions)
    // other plugins with `apply false` ...
}
```

---

## Plugins

### Overview

| Plugin | Alias | Use for |
|--------|-------|---------|
| `android-application-conventions` | `libs.plugins.android.application.conventions` | `:app` module (no Compose) |
| `android-compose-application-conventions` | `libs.plugins.android.compose.application.conventions` | `:app` module with Compose |
| `android-library-conventions` | `libs.plugins.android.library.conventions` | Standalone library modules |
| `android-feature-conventions` | `libs.plugins.android.feature.conventions` | Feature modules (no Compose) |
| `android-compose-library-conventions` | `libs.plugins.android.compose.library.conventions` | Library modules with Compose |
| `android-compose-feature-conventions` | `libs.plugins.android.compose.feature.conventions` | Feature modules with Compose |
| `android-project-conventions` | `libs.plugins.android.project.conventions` | Root project only |

### Plugin inheritance

```
android-application-conventions
└── android-compose-application-conventions

android-library-conventions
├── android-feature-conventions
└── android-compose-library-conventions
    └── android-compose-feature-conventions
```

---

## Usage by module type

### `:app` — Application with Compose

The most common case. Applies signing, build types, Compose, and KotlinX Serialization.

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.compose.application.conventions)
}

android {
    namespace = "com.example.myapp"

    defaultConfig {
        applicationId = "com.example.myapp"
    }
}
```

### `:core:*` — Library module (no Compose)

For modules that provide utilities, networking, data, etc. without a UI layer.

```kotlin
// core/network/build.gradle.kts
plugins {
    alias(libs.plugins.android.library.conventions)
}

android {
    namespace = "com.example.app.core.network"
}
```

### `:feature:*` or `:data:*` — Feature / data module with Compose

For feature and data modules that include Compose UI or depend on Compose infrastructure.

```kotlin
// feature/home/build.gradle.kts
plugins {
    alias(libs.plugins.android.compose.feature.conventions)
}

android {
    namespace = "com.example.app.feature.home"
}
```

### Root `build.gradle.kts`

Configures coverage reports and `project-report` across all subprojects.

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.android.project.conventions)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // ...
}
```

---

## What each plugin configures

### `android-application-conventions`

- Applies: `com.android.application`, `com.google.devtools.ksp`, `kotlin-parcelize`
- `compileSdk`, `minSdk`, `targetSdk` from version catalog
- `debug` signing config using `config/debug.keystore`
- `release` signing config loaded from `signing.release.properties`
- `debug`: minify disabled, unit test and Android test coverage enabled
- `release`: minify + resource shrinking enabled, proguard configured
- Java/Kotlin toolchain set from version catalog

### `android-compose-application-conventions`

Extends `android-application-conventions` and adds:
- `org.jetbrains.kotlin.plugin.compose`
- `org.jetbrains.kotlin.plugin.serialization`
- `buildFeatures.compose = true`
- `kotlinx-serialization-json` dependency

### `android-library-conventions`

- Applies: `com.android.library`, `com.google.devtools.ksp`, `kotlin-parcelize`
- `compileSdk`, `minSdk` from version catalog
- `debug`: coverage enabled (`enableUnitTestCoverage`, `enableAndroidTestCoverage`)
- Java/Kotlin toolchain set from version catalog

### `android-feature-conventions`

Thin wrapper — applies `android-library-conventions`. Use it to semantically distinguish feature modules from generic libraries.

### `android-compose-library-conventions`

Extends `android-library-conventions` and adds:
- `org.jetbrains.kotlin.plugin.compose`
- `buildFeatures.compose = true`

### `android-compose-feature-conventions`

Thin wrapper — applies `android-compose-library-conventions`. Use it for feature modules that include Compose UI.

### `android-project-conventions`

Root-project only. Configures:
- Code coverage aggregation via [`rootcoverage`](https://github.com/NeoTech-Software/Android-Root-Coverage-Plugin) (HTML + XML reports, unit tests + Android tests)
- `project-report` plugin on all subprojects
- `DownloadGradleDependencies` task

---

## Signing

`android-application-conventions` expects the following files at the root of the **host project**:

| Build type | File |
|------------|------|
| `debug` | `config/debug.keystore` (standard Android debug keystore) |
| `release` | `config/signing.release.properties` |

`signing.release.properties` format:

```properties
storeFile=config/release.keystore
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```
