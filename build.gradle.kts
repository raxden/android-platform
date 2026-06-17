import com.adarshr.gradle.testlogger.theme.ThemeType
import extension.getLibraryArtifactIds
import extension.getProperty
import extension.getPublicationName
import java.time.Duration
import java.time.temporal.ChronoUnit

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.versioning) apply false
    alias(libs.plugins.android.publish.library) apply false
    alias(libs.plugins.android.publish.platform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.test.logger)
    alias(libs.plugins.nmcp.aggregation)
    id("android-project-conventions")
}

nmcpAggregation {
    centralPortal {
        username = getProperty("CENTRAL_PORTAL_USERNAME")
        password = getProperty("CENTRAL_PORTAL_PASSWORD")

        // optional: publish manually from the portal {AUTOMATIC, USER_MANAGED}
        publishingType = getProperty("publishingType")

        // optional: configure the name of your publication in the portal UI
        publicationName = getPublicationName()

        // optional: increase the validation timeout to 30 minutes
        validationTimeout = Duration.of(30, ChronoUnit.MINUTES)

        when (getProperty("publishingType")) {
            "AUTOMATIC" -> {
                // optional: disable waiting for publishing and validation, and publish immediately after upload
                validationTimeout = Duration.ZERO
            }
        }

        // optional: send publications serially instead of in parallel (might be slower)
        uploadSnapshotsParallelism.set(1)
    }
}

dependencies {

    val selectedProjects = getProperty("nmcpProjects")
        .split(",")
        .map(String::trim)
        .filter(String::isNotBlank)

    selectedProjects.forEach { path ->
        nmcpAggregation(project(path))
    }
}

subprojects {
    apply(plugin = "com.adarshr.test-logger")
    pluginManager.withPlugin("maven-publish") { apply(plugin = "com.gradleup.nmcp") }

    group = "com.raxdenstudios"

    testlogger {
        theme = ThemeType.MOCHA
        slowThreshold = 3000
    }
}

// Prevent duplicate library artifacts on the classpath.
//
// This project produces all commons-* modules locally (projects.libraries.*). Transitive
// dependencies may, however, pull the same libraries as *published* artifacts from Maven
// Central (com.raxdenstudios:commons-*). Excluding those published coordinates guarantees
// that only the local source modules are used, avoiding version conflicts and duplicate
// classes. Project dependencies are unaffected because their module name is the Gradle
// project name (e.g. "core"), not the published artifactId (e.g. "commons-core").
//
// The list is derived dynamically from each library module's publish configuration, so new
// modules are picked up automatically. It must run inside projectsEvaluated so every module's
// publication is already configured before reading its artifactId.
gradle.projectsEvaluated {
    val libraryArtifactIds = getLibraryArtifactIds()
    subprojects.forEach { module ->
        module.configurations.all {
            libraryArtifactIds.forEach { artifactId ->
                exclude(group = "com.raxdenstudios", module = artifactId)
            }
        }
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
    }

    register("printKotlinVersion") {
        doLast {
            println(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
        }
    }
}
