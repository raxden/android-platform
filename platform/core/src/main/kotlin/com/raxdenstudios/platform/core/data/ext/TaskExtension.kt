package com.raxdenstudios.platform.core.data.ext

import com.google.android.gms.tasks.Task
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Time limit used by [awaitAnswer] and [awaitUnitAnswer] when the caller does not pass one.
 */
val DEFAULT_TASK_TIMEOUT: Duration = 10.seconds

/**
 * Suspends until this [Task] completes and returns its outcome as an [Answer], so the caller never hangs
 * on a Task that stays pending.
 *
 * The outcome is mapped as follows:
 * - Successful: [Answer.Success] with the Task result.
 * - Failed with an exception: [Answer.Failure] with a [Failure.Unknown] built by `toUnknownError`, prefixed
 *   with [errorMessage].
 * - Cancelled: [Answer.Failure] with a [Failure.Unknown] whose message ends in "task cancelled".
 * - Still pending after [timeout]: [Answer.Failure] with a [Failure.Unknown] whose message is
 *   "[errorMessage]: timed out after [timeout]".
 *
 * A Task cannot be cancelled. When the wait ends early (timeout or cancellation of the calling coroutine)
 * the Task keeps running and its late result is dropped. Pass [Duration.INFINITE] as [timeout] to wait
 * without a limit.
 *
 * ## Usage
 * ```kotlin
 * val answer: Answer<User, Failure> = task.awaitAnswer(errorMessage = "Load user")
 * answer.fold(
 *     onSuccess = { user -> showUser(user) },
 *     onFailure = { failure -> showError(failure.message) },
 * )
 * ```
 */
suspend fun <T> Task<T>.awaitAnswer(
    errorMessage: String,
    timeout: Duration = DEFAULT_TASK_TIMEOUT,
): Answer<T, Failure> = withTimeoutOrNull(timeout) { awaitCompletion(errorMessage) }
    ?: Answer.Failure(Failure.Unknown("$errorMessage: timed out after $timeout"))

/**
 * Same as [awaitAnswer] for a `Task<Void>`, such as the one returned by `FirebaseMessaging.subscribeToTopic`.
 *
 * A successful Task yields `Answer.Success(Unit)` because a Void result carries no value. Failures, cancellation
 * and timeouts are reported exactly as [awaitAnswer] reports them.
 *
 * ## Usage
 * ```kotlin
 * FirebaseMessaging.getInstance()
 *     .subscribeToTopic("news")
 *     .awaitUnitAnswer(errorMessage = "Subscribe to news")
 * ```
 */
suspend fun Task<Void>.awaitUnitAnswer(
    errorMessage: String,
    timeout: Duration = DEFAULT_TASK_TIMEOUT,
): Answer<Unit, Failure> = when (val answer = awaitAnswer(errorMessage, timeout)) {
    is Answer.Success -> Answer.Success(Unit)
    is Answer.Failure -> answer
}

// A Task cannot be cancelled: once the wait is cancelled (timeout or caller) its late result is dropped.
private suspend fun <T> Task<T>.awaitCompletion(
    errorMessage: String,
): Answer<T, Failure> = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) continuation.resume(task.toAnswer(errorMessage))
    }
}

private fun <T> Task<T>.toAnswer(
    errorMessage: String,
): Answer<T, Failure> = when {
    isSuccessful -> Answer.Success(result)
    isCanceled -> Answer.Failure(Failure.Unknown("$errorMessage: task cancelled"))
    else -> Answer.Failure(failureCause().toUnknownError(errorMessage))
}

private fun Task<*>.failureCause(): Throwable = exception ?: IllegalStateException("Task failed without exception")
