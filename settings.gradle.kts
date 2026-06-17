pluginManagement {
    includeBuild("build-logic")
    /**
     * The pluginManagement {repositories {...}} block configures the
     * repositories Gradle uses to search or download the Gradle plugins and
     * their transitive dependencies. Gradle pre-configures support for remote
     * repositories such as JCenter, Maven Central, and Ivy. You can also use
     * local repositories or define your own remote repositories. The code below
     * defines the Gradle Plugin Portal, Google's Maven repository,
     * and the Maven Central Repository as the repositories Gradle should use to look for its dependencies.
     */
    repositories {
        gradlePluginPortal()
        google()
        mavenLocal()
        mavenCentral()
        // Avoid mavenLocal in CI to keep builds reproducible
        if (!providers.environmentVariable("CI").isPresent) mavenLocal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libraries.versions.toml"))
        }
    }
    /**
     * The dependencyResolutionManagement { repositories {...}}
     * block is where you configure the repositories and dependencies used by
     * all modules in your project, such as libraries that you are using to
     * create your application. However, you should configure module-specific
     * dependencies in each module-level build.gradle file. For new projects,
     * Android Studio includes Google's Maven repository and the
     * Maven Central Repository by
     * default, but it does not configure any dependencies (unless you select a
     * template that requires some).
     */
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Avoid mavenLocal in CI to keep builds reproducible
        if (!providers.environmentVariable("CI").isPresent) mavenLocal()

        // Snapshots repo only for snapshot versions
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            mavenContent { snapshotsOnly() }
        }

        // JitPack for GitHub packages (checked after Maven Central)
        maven("https://jitpack.io")
    }
}

plugins {
    id("com.gradle.develocity") version "4.3.2"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"

        // Publishing a build scan for every build execution
        publishing.onlyIf { providers.environmentVariable("CI").isPresent }
    }
}

//include(":bom")
include(":bom")

include(":platform:core")
include(":platform:device")
include(":platform:network")
include(":platform:ui")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
