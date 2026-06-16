package extension

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import java.io.File
import java.net.URL

private const val PROGRESS_STEP_PERCENT = 5
private const val PROGRESS_BAR_SEGMENTS = 20

fun Project.downloadRepository(
    repository: URL,
    destination: File,
): File {
    val zipFile = File("${rootDir.path}/${repository.path.hashCode()}.zip")
    val outputDir = destination.also {
        it.deleteRecursively()
        it.mkdirs()
    }

    repository.downloadTo(zipFile) { percent, mb, totalMb ->
        if (percent != null && totalMb != null) {
            val filled = percent / PROGRESS_STEP_PERCENT
            val bar = "█".repeat(filled) + "░".repeat(PROGRESS_BAR_SEGMENTS - filled)
            println("  │  [$bar] $percent% (%.2f / %.2f MB)".format(mb, totalMb))
        } else {
            println("  │  Downloading... %.2f MB".format(mb))
        }
    }

    copy {
        from(zipTree(zipFile))
        into(outputDir)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    outputDir.listFiles()?.firstOrNull()?.run {
        listFiles()?.forEach { file -> file.renameTo(File(outputDir, file.name)) }
        deleteRecursively()
    }

    zipFile.delete()

    return outputDir
}
