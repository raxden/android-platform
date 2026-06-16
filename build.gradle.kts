import com.adarshr.gradle.testlogger.theme.ThemeType
import extension.getProperty
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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

    configurations.all {
        exclude(group = "com.raxdenstudios", module = "platform-core")
        exclude(group = "com.raxdenstudios", module = "platform-device")
        exclude(group = "com.raxdenstudios", module = "platform-network")
        exclude(group = "com.raxdenstudios", module = "platform-ui")
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

private fun getPublicationName(): String {
    val gitShortSha = runCatching {
        val p = ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
            .redirectErrorStream(true)
            .start()
        val out = p.inputStream.bufferedReader().readText().trim()
        if (p.waitFor() == 0 && out.isNotBlank()) out else null
    }.getOrNull()
    val utcStamp = ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"))
    return listOfNotNull(utcStamp, gitShortSha).joinToString("+")
}
