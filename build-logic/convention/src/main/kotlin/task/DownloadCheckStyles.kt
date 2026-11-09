package task

import extension.downloadRepository
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File
import java.net.URI

open class DownloadCheckStyles : DefaultTask() {

    @TaskAction
    fun execute() {
        downloadCheckStyles()
    }

    private fun downloadCheckStyles() {
        val separator = "─".repeat(SEPARATOR_LENGTH)
        println()
        println("  ┌$separator")
        println("  │  Downloading Checkstyles")
        println("  │  $GIT_CONVENTIONS_SOURCE")
        println("  ├$separator")

        val source = URI.create(GIT_CONVENTIONS_SOURCE).toURL()
        val tempDir = File(project.rootDir.path + "/.checkstyles_tmp/")
        project.downloadRepository(source, tempDir, excludes = listOf("**/.github/**"))

        val configSource = File(tempDir, "config")
        val destination = File(project.rootDir.path + "/config/")
        destination.mkdirs()

        configSource.listFiles()?.forEach { sourceSubDir ->
            File(destination, sourceSubDir.name).deleteRecursively()
            sourceSubDir.renameTo(File(destination, sourceSubDir.name))
        }
        tempDir.deleteRecursively()

        println("  ├$separator")
        println("  │  Saved to ${destination.absolutePath}")
        println("  └$separator")
        println()
    }

    companion object {

        private const val TASK_NAME = "downloadCheckStyles"
        private const val GROUP_TASK_NAME = "dependencies"
        private const val GIT_CONVENTIONS_SOURCE =
            "https://github.com/raxden/android-convention/archive/refs/heads/master.zip"

        private const val SEPARATOR_LENGTH = 85

        fun register(project: Project) {
            project.tasks.register<DownloadCheckStyles>(TASK_NAME) {
                group = GROUP_TASK_NAME
            }
        }
    }
}
