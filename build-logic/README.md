# Android Convention

[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A collection of Gradle **convention plugins** for Android projects, designed to keep a single source of truth for common module configurations.

This approach is based on:
- [Herding Elephants – Square Engineering](https://developer.squareup.com/blog/herding-elephants/)
- [Idiomatic Gradle – Johannes Hüsing](https://github.com/jjohannes/idiomatic-gradle)

Convention plugins avoid duplicated build script setup and messy `subprojects` blocks, without the pitfalls of `buildSrc`. They are **additive** and **composable** — each plugin has a single responsibility, and modules pick only what they need. One-off logic that isn't shared should stay directly in the module's `build.gradle.kts`.

## Table of contents

- [Why convention plugins?](#why-convention-plugins)
- [Requirements](#requirements)
- [Structure](#structure)
- [Setup](#setup)
- [Plugins](#plugins)
- [Usage by module type](#usage-by-module-type)
- [What each plugin configures](#what-each-plugin-configures)
- [Dependency bundle helpers](#dependency-bundle-helpers)
- [Tasks](#tasks)
- [Signing](#signing)
- [Updating the submodule](#updating-the-submodule)

## Why convention plugins?

| Approach | Drawback |
|----------|----------|
| Copy/paste build config | Drifts out of sync across modules |
| `subprojects {}` / `allprojects {}` | Forces config on modules that don't need it, hurts caching |
| `buildSrc` | Invalidates the whole build cache on any change |
| **Convention plugins** ✅ | Opt-in, composable, cache-friendly, single responsibility |

## Requirements

| Tool | Version |
|------|---------|
| **JDK** | 17 |
| **Min SDK** | 24 |
| **Gradle** | 8.x+ (uses `includeBuild` / composite builds) |
| **AGP / Kotlin** | Provided by the host project's version catalog |

> All SDK, JDK and toolchain versions are read from the host project's `gradle/libraries.versions.toml`. See [`model/Versions.kt`](convention/src/main/kotlin/model/Versions.kt) for the keys consumed by the plugins.

## Structure

```
android-convention/
├── convention/                  # Convention plugins module
│   ├── build.gradle.kts         # Plugin classpath (AGP, Kotlin, KSP, coverage…)
│   └── src/main/kotlin/
│       ├── android-application-conventions.gradle.kts
│       ├── android-library-conventions.gradle.kts
│       ├── android-feature-conventions.gradle.kts
│       ├── android-compose-application-conventions.gradle.kts
│       ├── android-compose-library-conventions.gradle.kts
│       ├── android-compose-feature-conventions.gradle.kts
│       ├── android-project-conventions.gradle.kts
│       ├── extension/           # Shared Kotlin helpers
│       │   ├── CommonExtension.kt           # android {} accessor helpers
│       │   ├── DependencyExtension.kt       # *Bundle dependency helpers
│       │   ├── ProjectExtension.kt          # getProperty / signing props
│       │   ├── URLExtension.kt              # repo download helper
│       │   └── VersionCatalogExtensions.kt  # libs / versions accessors
│       ├── model/
│       │   └── Versions.kt      # Type-safe version-catalog keys
│       └── task/
│           ├── DownloadGradleDependencies.kt # Refresh build-logic from remote
│           └── DownloadGithubActions.kt      # Sync shared GitHub Actions
├── gradle/
│   └── libraries.versions.toml  # Fallback version catalog
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
- `buildFeatures.buildConfig = true`
- `testInstrumentationRunner = androidx.test.runner.AndroidJUnitRunner`
- `debug` signing config using `config/debug.keystore` (alias `androiddebugkey`)
- `release` signing config loaded from `config/signing.release.properties`
- `debug`: `applicationIdSuffix = ".debug"`, minify disabled, unit + Android test coverage enabled
- `release`: minify + resource shrinking enabled, proguard configured
- Packaging excludes for common `META-INF` license/kotlin-module clashes
- Java/Kotlin toolchain (`jvmToolchain`) set from version catalog

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
- Code coverage aggregation via [`rootcoverage`](https://github.com/NeoTech-Software/Android-Root-Coverage-Plugin) (HTML + XML reports, unit + Android test results), with sensible `excludes` for DI, generated, Compose and Android-framework classes
- `project-report` plugin on all subprojects
- Registers the [`downloadGradleDependencies`](#tasks) and [`downloadGithubActions`](#tasks) tasks

> The build variant used by coverage is controlled by the `buildType` Gradle property (defaults to `debug`).

---

## Dependency bundle helpers

`extension/DependencyExtension.kt` provides helpers that expand a version-catalog **bundle** and route each dependency to the correct configuration automatically:

- Entries whose name contains `bom` are added with `platform(...)`
- Entries whose name contains `compiler` are added to the matching KSP configuration
- Everything else is added to the standard configuration

| Helper | Configuration | KSP target |
|--------|---------------|-----------|
| `implementationBundle(...)` | `implementation` | `ksp` |
| `debugImplementationBundle(...)` | `debugImplementation` | — |
| `androidTestImplementationBundle(...)` | `androidTestImplementation` | `kspAndroidTest` |

```kotlin
import extension.implementationBundle

dependencies {
    // A bundle that may mix a BOM, a compiler (KSP) and regular libraries —
    // each entry is dispatched to the right configuration automatically.
    implementationBundle(libs.bundles.room)
}
```

---

## Tasks

Registered by `android-project-conventions` under the `dependencies` group:

| Task | Description |
|------|-------------|
| `downloadGradleDependencies` | Downloads the latest `android-convention` archive into `build-logic/`, refreshing the convention plugins. |
| `downloadGithubActions` | Downloads the shared [`android-github-actions`](https://github.com/raxden/android-github-actions) repo and syncs its contents into the host project's `.github/`. |

```sh
./gradlew downloadGradleDependencies
./gradlew downloadGithubActions
```

> Coverage report (from the `rootcoverage` plugin):
>
> ```sh
> ./gradlew rootCoverageReport
> ```

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

> Keep `config/signing.release.properties` and `config/release.keystore` **out of version control**. The `debug` keystore uses the well-known Android defaults and is safe to commit.

---

## Updating the submodule

Since `build-logic` is a Git submodule, pull the latest convention plugins with:

```sh
git submodule update --remote build-logic
```

Or, without managing the submodule manually, run the bundled task to re-download the latest version into `build-logic/`:

```sh
./gradlew downloadGradleDependencies
```

---

## License

```
Copyright 2022 Ángel Gómez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
