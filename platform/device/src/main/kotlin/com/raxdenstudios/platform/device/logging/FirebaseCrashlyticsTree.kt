package com.raxdenstudios.platform.device.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Timber tree that reports errors to Firebase Crashlytics as non-fatal exceptions. Intended for release builds.
 *
 * Only logs at [Log.INFO] or above are accepted, and only logs that carry a [Throwable] are recorded; messages
 * without a throwable are not sent. Connectivity failures (`UnknownHostException`, `ConnectException`,
 * `SocketTimeoutException`) are skipped because they are expected noise when the device is offline. A log counts
 * as a connectivity failure when the throwable, a cause in its chain or the log message names one of those types.
 *
 * ## Setup
 * ```kotlin
 * if (!BuildConfig.DEBUG) Timber.plant(FirebaseCrashlyticsTree())
 * ```
 *
 * @property crashlytics Provides the Crashlytics instance. It is invoked for every recorded log, not once at
 * construction.
 */
class FirebaseCrashlyticsTree(
    private val crashlytics: () -> FirebaseCrashlytics = { FirebaseCrashlytics.getInstance() },
) : Timber.Tree() {

    override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean = priority >= Log.INFO

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (t == null || t.isConnectivityError() || message.mentionsConnectivityError()) return
        crashlytics().recordException(t)
    }

    private fun Throwable.isConnectivityError(): Boolean =
        generateSequence(this) { throwable -> throwable.cause?.takeIf { cause -> cause !== throwable } }
            .take(MAX_CAUSE_DEPTH)
            .any { throwable -> CONNECTIVITY_ERRORS.any { type -> type.isInstance(throwable) } }

    private fun String.mentionsConnectivityError(): Boolean = CONNECTIVITY_ERRORS.any { type -> contains(type.name) }

    private companion object {
        /** Bounds the cause-chain walk so a very deep or unusual chain cannot make each log call expensive. */
        const val MAX_CAUSE_DEPTH = 10
        val CONNECTIVITY_ERRORS = listOf(
            UnknownHostException::class.java,
            ConnectException::class.java,
            SocketTimeoutException::class.java,
        )
    }
}
