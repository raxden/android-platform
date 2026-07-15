package com.raxdenstudios.platform.core.domain.ext

import com.raxdenstudios.platform.core.domain.model.Failure

fun Throwable.toUnknownError(errorMessage: String): Failure.Unknown =
    Failure.Unknown("$errorMessage: $message")
