package task

import extension.downloadRepository
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File
import java.net.URI

open class DownloadGithubActions : DefaultTask() {

    @TaskAction
    fun execute() {
        downloadGithubActionsRepository()
    }

    private fun downloadGithubActionsRepository() {
        val separator = "─".repeat(SEPARATOR_LENGTH)
        println()
        println("  ┌$separator")
        println("  │  Downloading Github Actions")
        println("  │  $GIT_GITHUB_ACTIONS_SOURCE")
        println("  ├$separator")

        val source = URI.create(GIT_GITHUB_ACTIONS_SOURCE).toURL()
        val tempDir = File(project.rootDir.path + "/.github_tmp/")
        project.downloadRepository(source, tempDir)

        val githubSource = File(tempDir, ".github")
        val destination = File(project.rootDir.path + "/.github/")
        destination.mkdirs()

        githubSource.listFiles()?.forEach { sourceSubDir ->
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

        private const val TASK_NAME = "downloadGithubActions"
        private const val GROUP_TASK_NAME = "dependencies"
        private const val GIT_GITHUB_ACTIONS_SOURCE =
            "https://github.com/raxden/android-github-actions/archive/refs/heads/master.zip"

        private const val SEPARATOR_LENGTH = 85

        fun register(project: Project) {
            project.tasks.register<DownloadGithubActions>(TASK_NAME) {
                group = GROUP_TASK_NAME
            }
        }
    }
}
