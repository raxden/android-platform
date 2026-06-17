package extension

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val GIT_SHORT_SHA_LENGTH = 7
private const val TIMESTAMP_PATTERN = "yyyyMMdd.HHmmss"
private const val PUBLISH_LIBRARY_EXTENSION = "publishLibrary"

/**
 * Builds a unique publication name combining a UTC timestamp with the current
 * git short SHA, e.g. `20260617.095700+a1b2c3d`.
 *
 * If git is unavailable the SHA is omitted and only the timestamp is returned.
 */
fun Project.getPublicationName(): String {
    val gitShortSha = runCatching {
        val process = ProcessBuilder("git", "rev-parse", "--short=$GIT_SHORT_SHA_LENGTH", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() == 0 && output.isNotBlank()) output else null
    }.getOrNull()

    val utcStamp = ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN))

    return listOfNotNull(utcStamp, gitShortSha).joinToString("+")
}

/**
 * Returns the published Maven artifactIds of every library module, i.e. subprojects that apply the
 * `publishLibrary` plugin. The BOM module (which applies `publishPlatform`) is intentionally
 * excluded.
 *
 * Must be called once every project is evaluated (e.g. inside `gradle.projectsEvaluated { }`) so
 * the Maven publications are already configured.
 */
fun Project.getLibraryArtifactIds(): List<String> =
    libraryModules().flatMap { module ->
        module.extensions.findByType(PublishingExtension::class.java)
            ?.publications
            ?.withType(MavenPublication::class.java)
            ?.map { it.artifactId }
            .orEmpty()
    }

/**
 * Library modules of the project: subprojects that apply the `publishLibrary` plugin. Detected by
 * the presence of the `publishLibrary` extension to avoid a compile-time dependency on the
 * publishing plugin.
 */
fun Project.libraryModules(): List<Project> =
    subprojects.filter { it.extensions.findByName(PUBLISH_LIBRARY_EXTENSION) != null }
