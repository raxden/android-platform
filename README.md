# Android Platform

[![CI](https://github.com/raxden/android-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/raxden/android-platform/actions/workflows/ci.yml)
[![Publish](https://github.com/raxden/android-platform/actions/workflows/ci_publish.yml/badge.svg)](https://github.com/raxden/android-platform/actions/workflows/ci_publish.yml)
[![codecov](https://codecov.io/gh/raxden/android-platform/branch/master/graph/badge.svg)](https://codecov.io/gh/raxden/android-platform)
[![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/platform-bom.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.raxdenstudios/platform-bom)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A collection of opinionated Android platform libraries providing foundational building blocks for modern Android app development.

## 📋 Overview

Android Platform is a multi-module library that provides a curated set of platform-level abstractions and UI components built on top of Jetpack Compose, Kotlin Coroutines, Koin, and Firebase. It is designed to be consumed as a BOM so all modules stay version-compatible.

## 📚 Modules

### Platform modules

| Artifact ID | Description | Latest Version |
|---|---|---|
| `platform-bom` | Bill of Materials — manages all module versions | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/platform-bom.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/platform-bom) |
| `platform-core` | Core utilities, DI setup, Room, and Compose base | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/platform-core.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/platform-core) |
| `platform-ui` | Jetpack Compose UI components and navigation utilities | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/platform-ui.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/platform-ui) |
| `platform-device` | Device-level abstractions (location, Firebase, permissions) | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/platform-device.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/platform-device) |
| `platform-network` | Network layer with OkHttp/Retrofit, Chucker debug support | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/platform-network.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/platform-network) |

## 🚀 Getting Started

### Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Kotlin**: 2.0+
- **Java**: 17

### Installation

#### Using BOM (recommended)

```kotlin
dependencies {
    implementation(platform("com.raxdenstudios:platform-bom:<latest-version>"))

    implementation("com.raxdenstudios:platform-core")
    implementation("com.raxdenstudios:platform-ui")
    implementation("com.raxdenstudios:platform-device")
    implementation("com.raxdenstudios:platform-network")
}
```

#### Individual modules (without BOM)

```kotlin
dependencies {
    implementation("com.raxdenstudios:platform-core:<version>")
    implementation("com.raxdenstudios:platform-ui:<version>")
    implementation("com.raxdenstudios:platform-device:<version>")
    implementation("com.raxdenstudios:platform-network:<version>")
}
```

## 🧪 Testing

Run unit tests:

```bash
./gradlew test
```

Generate JaCoCo coverage reports:

```bash
./gradlew jacocoTestReport
```

## ⚙️ CI/CD

| Workflow | Trigger | Description |
|---|---|---|
| `ci.yml` | Pull Request | Runs checkstyle, build, and unit tests |
| `ci_publish.yml` | Push to `master` | Bumps versions, publishes to Maven Central |
| `ci_bump.yml` | Manual | Bumps module versions manually |
| `ci_reports.yml` | Push to `master` | Generates and uploads coverage reports to Codecov |

See [`config/`](config/) for the GitHub secrets and variables required to run these workflows.

## 📄 License

```
Copyright 2015 Ángel Gómez

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
