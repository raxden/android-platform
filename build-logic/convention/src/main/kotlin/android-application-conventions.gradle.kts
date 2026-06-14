import extension.getSigningConfigProperties
import extension.libs
import extension.versions

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {

    compileSdk = libs.versions.compileSdk

    compileOptions {
        sourceCompatibility = libs.versions.sourceCompatibility
        targetCompatibility = libs.versions.targetCompatibility
    }

    signingConfigs {
        getByName("debug") {
            storeFile = project.file("${project.rootDir}/config/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            project.getSigningConfigProperties("release").run {
                storeFile = project.file("${project.rootDir}/${getProperty("storeFile")}")
                storePassword = getProperty("storePassword")
                keyAlias = getProperty("keyAlias")
                keyPassword = getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk
        targetSdk = libs.versions.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")

            // Proguard configuration
            isMinifyEnabled = false

            // Tests configuration
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")

            // Proguard configuration
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE-notice.md")
            excludes.add("META-INF/*.kotlin_module")
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