# Android Commons

[![Continuous Delivery](https://github.com/raxden/android-commons/workflows/Continuous%20Delivery/badge.svg)](https://github.com/raxden/android-commons/actions/workflows/ci_publish.yml)
[![codecov](https://codecov.io/gh/raxden/android-commons/branch/master/graph/badge.svg?token=E55S5DHJ9B)](https://codecov.io/gh/raxden/android-commons)
[![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-bom.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-bom)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A comprehensive collection of modular Android libraries providing utilities, extensions, and common functionality for modern Android development.

## 📋 Overview

Android Commons is a curated set of libraries designed to accelerate Android development by providing reusable components and utilities. Each module is independent, allowing you to include only what you need, keeping your app size minimal.

### ✨ Key Features

- 🎯 **Modular Architecture** - Include only the modules you need
- 🚀 **Modern Android** - Built with Kotlin, Coroutines, and Jetpack Compose
- 🧪 **Well Tested** - Comprehensive unit test coverage
- 📦 **Small Footprint** - Minimal dependencies per module
- 🔄 **Active Maintenance** - Regular updates and improvements

## 📚 Available Modules

| Module | Description | Documentation | Latest Version |
|--------|-------------|---------------|----------------|
| **Bill of Materials** |
| `commons-bom` | BOM for version management | - | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-bom.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-bom) |
| **Core & Android** |
| `commons-core` | Core utilities and extensions | - | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-core.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-core) |
| `commons-android` | Android-specific utilities and extensions | - | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-android.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-android) |
| `commons-android-binding` | View binding utilities | - | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-android-binding.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-android-binding) |
| `commons-android-compose` | Jetpack Compose utilities | - | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-android-compose.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-android-compose) |
| `commons-android-test` | Android testing utilities | - | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-android-test.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-android-test) |
| **Async & Concurrency** |
| `commons-coroutines` | Kotlin Coroutines extensions | [📖 Docs](libraries/coroutines/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-coroutines.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-coroutines) |
| `commons-coroutines-test` | Coroutines testing utilities | [📖 Docs](libraries/coroutines/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-coroutines-test.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-coroutines-test) |
| **Networking** |
| `commons-network` | Network utilities and interceptors | [📖 Docs](libraries/network/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-network.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-network) |
| **Pagination** |
| `commons-pagination` | Base pagination framework | [📖 Docs](libraries/pagination/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-pagination.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-pagination) |
| `commons-pagination-co` | Coroutines-based pagination | [📖 Docs](libraries/pagination/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-pagination-co.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-pagination-co) |
| **Permissions** |
| `commons-permissions` | Runtime permissions management | [📖 Docs](libraries/permissions/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-permissions.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-permissions) |
| **Storage** |
| `commons-preferences` | Advanced SharedPreferences wrapper | [📖 Docs](libraries/preferences/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-preferences.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-preferences) |
| **Date & Time** |
| `commons-threetenabp` | ThreeTenABP date/time utilities | - | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-threetenabp.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-threetenabp) |
| `commons-threetenabp-test` | ThreeTenABP testing utilities | - | [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-threetenabp-test.svg?label=version)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-threetenabp-test) |

## 🚀 Getting Started

### Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Kotlin**: 1.9+
- **Java**: 17

### Installation

#### Using BOM (Bill of Materials) 🎯

The **Bill of Materials (BOM)** manages all library versions automatically, ensuring compatibility between modules.

**Latest BOM version:** [![Maven Central](https://img.shields.io/maven-central/v/com.raxdenstudios/commons-bom.svg)](https://central.sonatype.com/artifact/com.raxdenstudios/commons-bom)

```kotlin
dependencies {
    // Import the BOM with the latest version (check badge above)
    implementation(platform("com.raxdenstudios:commons-bom:<latest-version>"))
    
    // Add modules without specifying versions
    implementation("com.raxdenstudios:commons-core")
    implementation("com.raxdenstudios:commons-android")
    implementation("com.raxdenstudios:commons-coroutines")
    implementation("com.raxdenstudios:commons-network")
    implementation("com.raxdenstudios:commons-pagination")
    implementation("com.raxdenstudios:commons-pagination-co")
    implementation("com.raxdenstudios:commons-permissions")
    implementation("com.raxdenstudios:commons-preferences")
    implementation("com.raxdenstudios:commons-threetenabp")
    
    // Testing modules
    testImplementation("com.raxdenstudios:commons-coroutines-test")
    testImplementation("com.raxdenstudios:commons-threetenabp-test")
    testImplementation("com.raxdenstudios:commons-android-test")
}
```

**Benefits of using BOM:**
- ✅ No need to specify versions for each module
- ✅ Guaranteed compatibility between all modules
- ✅ Easier updates - just change the BOM version
- ✅ Prevents version conflicts

## 🧪 Testing

All modules include comprehensive unit tests. Run tests with:

```bash
./gradlew test
```

For coverage reports:

```bash
./gradlew jacocoTestReport
```

## 📖 Documentation

Detailed documentation for each module is available in their respective directories:

- [Core](libraries/core/README.md)
- [Android](libraries/android/README.md)
- [Coroutines](libraries/coroutines/README.md)
- [Network](libraries/network/README.md)
- [Pagination](libraries/pagination/README.md)
- [Permissions](libraries/permissions/README.md)
- [Preferences](libraries/preferences/README.md)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

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
