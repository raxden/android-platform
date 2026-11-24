package com.raxdenstudios.platform.core.domain.ext

import com.raxdenstudios.platform.core.domain.model.Failure

/**
 * Wraps this throwable as a [Failure.Unknown] for the operation described by [errorMessage].
 *
 * The failure message is `"<errorMessage>: <throwable message or class simple name>"`. This throwable is kept as
 * the failure's `cause` for logging and reporting.
 */
fun Throwable.toUnknownError(
    errorMessage: String,
): Failure.Unknown = Failure.Unknown(
    message = "$errorMessage: ${message ?: javaClass.simpleName.ifEmpty { javaClass.name }}",
    cause = this,
)
