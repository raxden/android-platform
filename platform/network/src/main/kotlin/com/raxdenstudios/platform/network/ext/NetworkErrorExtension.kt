package com.raxdenstudios.platform.network.ext

import com.raxdenstudios.commons.core.NetworkError
import com.raxdenstudios.platform.core.domain.model.Failure

/**
 * Converts a commons [NetworkError] into the platform [Failure.Network] that the domain layer consumes.
 *
 * The error kind is kept: [NetworkError.Client] becomes [Failure.Network.Client], [NetworkError.Server] becomes
 * [Failure.Network.Server], [NetworkError.Network] becomes [Failure.Network.Connection] and [NetworkError.Unknown]
 * becomes [Failure.Network.Unknown]. Only the message is carried over; the HTTP status code and the response body
 * are dropped, so code that needs them must read the [NetworkError] before converting it.
 *
 * ## Usage
 * ```kotlin
 * // response: NetworkResponse<User, ErrorBody>
 * val answer: Answer<User, Failure.Network> = response
 *     .toAnswer(errorMessage = "Load user")
 *     .thenFailure { error -> error.toFailure() }
 * ```
 */
fun NetworkError<*>.toFailure(): Failure.Network = when (this) {
    is NetworkError.Client -> Failure.Network.Client(message)
    is NetworkError.Network -> Failure.Network.Connection(message)
    is NetworkError.Server -> Failure.Network.Server(message)
    is NetworkError.Unknown -> Failure.Network.Unknown(message)
}
