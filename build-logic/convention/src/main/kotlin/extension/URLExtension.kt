package extension

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val BUFFER_SIZE_BYTES = 8 * 1024
private const val BYTES_PER_KB = 1024.0
private const val BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB
private const val PROGRESS_STEP_PERCENT = 5
private const val MAX_PERCENT = 100
private const val PROGRESS_STEP_BYTES = BYTES_PER_MB.toLong()

typealias ProgressListener = (percent: Int?, downloadedMb: Double, totalMb: Double?) -> Unit

fun URL.downloadTo(destination: File, onProgress: ProgressListener? = null) {
    val connection = openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = true
    connection.connect()
    val totalBytes = connection.contentLengthLong
    var downloadedBytes = 0L
    val buffer = ByteArray(BUFFER_SIZE_BYTES)
    var lastPrintedPercent = -1
    var lastPrintedMb = 0L

    try {
        connection.inputStream.use { input ->
            destination.outputStream().use { output ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (onProgress != null) {
                        if (totalBytes > 0) {
                            val percent = (downloadedBytes * MAX_PERCENT / totalBytes).toInt()
                            if (percent != lastPrintedPercent && percent % PROGRESS_STEP_PERCENT == 0) {
                                lastPrintedPercent = percent
                                onProgress(percent, downloadedBytes / BYTES_PER_MB, totalBytes / BYTES_PER_MB)
                            }
                        } else {
                            val downloadedMbStep = downloadedBytes / PROGRESS_STEP_BYTES
                            if (downloadedMbStep != lastPrintedMb) {
                                lastPrintedMb = downloadedMbStep
                                onProgress(null, downloadedBytes / BYTES_PER_MB, null)
                            }
                        }
                    }
                }
            }
        }
        if (onProgress != null) {
            val totalMb = if (totalBytes > 0) totalBytes / BYTES_PER_MB else null
            onProgress(if (totalBytes > 0) MAX_PERCENT else null, downloadedBytes / BYTES_PER_MB, totalMb)
        }
    } finally {
        connection.disconnect()
    }
}
