package com.raxdenstudios.platform.device.logging

import com.raxdenstudios.commons.coroutines.CoroutineLogger
import com.raxdenstudios.commons.coroutines.ext.defaultCoroutineLogger
import timber.log.Timber

/**
 * [CoroutineLogger] that reports uncaught coroutine errors through Timber at error level.
 *
 * Each error is logged with the tag supplied by the commons coroutine helpers (`CoroutineException` for
 * `defaultExceptionHandler` and `safeLaunch`) and with the throwable attached. Entries go to whichever Timber trees
 * are planted when the error happens; the order of planting and [install] does not matter. Without a planted
 * tree nothing is logged.
 *
 * ## Setup
 * ```kotlin
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Timber.plant(FirebaseCrashlyticsTree())
 *         TimberCoroutineLogger.install()
 *     }
 * }
 * ```
 */
class TimberCoroutineLogger : CoroutineLogger {

    override fun logError(
        tag: String,
        message: String?,
        throwable: Throwable,
    ) {
        Timber.tag(tag).e(throwable, message)
    }

    companion object {

        /**
         * Sets [defaultCoroutineLogger] to a new [TimberCoroutineLogger], so uncaught errors from the commons coroutine
         * helpers are reported through Timber. Call once at app start; planting order does not matter. Calling it
         * again replaces the logger that was installed before.
         */
        fun install() {
            defaultCoroutineLogger = TimberCoroutineLogger()
        }
    }
}
