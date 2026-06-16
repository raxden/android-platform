import extension.libs
import extension.versions

plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {

    compileSdk = libs.versions.compileSdk

    compileOptions {
        sourceCompatibility = libs.versions.sourceCompatibility
        targetCompatibility = libs.versions.targetCompatibility
    }

    defaultConfig {
        minSdk = libs.versions.minSdk
    }

    testOptions {
        // used for instrumental tests
        targetSdk = libs.versions.targetSdk
    }

    buildTypes {
        getByName("debug") {
            // Proguard configuration
            isMinifyEnabled = false

            // Tests configuration
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    packaging {
        resources {
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE-notice.md")
//            excludes.add("META-INF/*.kotlin_module")
        }
    }
}

kotlin {
    jvmToolchain(jdkVersion = libs.versions.jdk.asInt())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.asInt()))
    }
}