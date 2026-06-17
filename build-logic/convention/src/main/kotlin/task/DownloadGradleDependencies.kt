package task

import extension.downloadRepository
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File
import java.net.URI

open class DownloadGradleDependencies : DefaultTask() {

    @TaskAction
    fun execute() {
        downloadConventionsRepository()
    }

    private fun downloadConventionsRepository() {
        val separator = "─".repeat(SEPARATOR_LENGTH)
        println()
        println("  ┌$separator")
        println("  │  Downloading Gradle Conventions")
        println("  │  $GIT_CONVENTIONS_SOURCE")
        println("  ├$separator")

        val source = URI.create(GIT_CONVENTIONS_SOURCE).toURL()
        val destination = File(project.rootDir.path + "/build-logic/")
        val outputDir = project.downloadRepository(
            repository = source,
            destination = destination,
            excludes = listOf("**/.github/**")
        )

        syncVersionCatalog(outputDir)

        println("  ├$separator")
        println("  │  Saved to ${outputDir.absolutePath}")
        println("  └$separator")
        println()
    }

    private fun syncVersionCatalog(buildLogicDir: File) {
        val source = File(buildLogicDir, "gradle/libraries.versions.toml")
        if (!source.exists()) return
        val destination = File(project.rootDir.path + "/gradle/libraries.versions.toml")
        source.copyTo(destination, overwrite = true)
        println("  │  Synced libraries.versions.toml → ${destination.absolutePath}")

        File(buildLogicDir, "gradle").deleteRecursively()
        println("  │  Removed ${buildLogicDir.absolutePath}/gradle")

        File(buildLogicDir, "gradle.properties").delete()
        println("  │  Removed ${buildLogicDir.absolutePath}/gradle.properties")
    }

    companion object {

        private const val TASK_NAME = "downloadGradleDependencies"
        private const val GROUP_TASK_NAME = "dependencies"
        private const val GIT_CONVENTIONS_SOURCE =
            "https://github.com/raxden/android-convention/archive/refs/heads/master.zip"

        private const val SEPARATOR_LENGTH = 85

        fun register(project: Project) {
            project.tasks.register<DownloadGradleDependencies>(TASK_NAME) {
                group = GROUP_TASK_NAME
            }
        }
    }
}
