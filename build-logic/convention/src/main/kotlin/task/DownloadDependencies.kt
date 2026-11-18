package task

import org.gradle.api.Project

/**
 * Aggregate task that refreshes every shared artifact in one run:
 * `downloadGradleDependencies`, `downloadGithubActions` and `downloadCheckStyles`.
 *
 * Register it after the tasks it depends on.
 */
object DownloadDependencies {

    private const val TASK_NAME = "downloadDependencies"
    private const val GROUP_TASK_NAME = "dependencies"

    fun register(project: Project) {
        project.tasks.register(TASK_NAME) {
            group = GROUP_TASK_NAME
            description = "Runs downloadGradleDependencies, downloadGithubActions and downloadCheckStyles."
            dependsOn(
                DownloadGradleDependencies.TASK_NAME,
                DownloadGithubActions.TASK_NAME,
                DownloadCheckStyles.TASK_NAME,
            )
        }
    }
}
