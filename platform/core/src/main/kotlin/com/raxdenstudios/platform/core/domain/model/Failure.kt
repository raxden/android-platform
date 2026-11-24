package com.raxdenstudios.platform.core.domain.model

/**
 * Domain error carried in `Answer<T, Failure>` when an operation does not succeed.
 *
 * The interface is open on purpose: apps add their own business failures as `sealed interface XFailure : Failure`
 * next to the shared subtypes below. [message] is diagnostic text for logs, not user-facing copy; the UI chooses
 * its text by the failure type.
 *
 * [cause] is the technical origin of the failure, for diagnosis and reporting only, e.g.
 * `failure.cause?.let { Timber.e(it, failure.message) }`. It is not part of equality. Never keep a [Failure] in UI
 * state or `SavedStateHandle`: map it to an `ErrorModel` and drop it, so the throwable is not retained.
 */
interface Failure {

    /**
     * Diagnostic text for logs. It is not user-facing copy.
     */
    val message: String

    /**
     * Technical origin of this failure, or `null` when it is not known. Used for diagnosis and reporting only.
     */
    val cause: Throwable?
        get() = null

    /**
     * Failure of a network request, split by HTTP status class or transport problem.
     */
    sealed interface Network : Failure {

        /**
         * HTTP 4xx response from the server.
         */
        data class Client(
            override val message: String,
        ) : Network

        /**
         * HTTP 5xx response from the server.
         */
        data class Server(
            override val message: String,
        ) : Network

        /**
         * No connectivity, or the request timed out before reaching the server.
         */
        data class Connection(
            override val message: String,
        ) : Network

        /**
         * Network error that matches none of the other network subtypes.
         */
        data class Unknown(
            override val message: String,
        ) : Network
    }

    /**
     * The caller is not authenticated or not allowed to access the resource.
     */
    data class Unauthorized(
        override val message: String,
    ) : Failure

    /**
     * The requested resource does not exist.
     */
    data class ResourceNotFound(
        override val message: String,
    ) : Failure

    /**
     * An unexpected error that does not fit any other subtype.
     *
     * Use the secondary constructor to keep the throwable that caused it. `copy()` does not carry the [cause]
     * over, because it goes through the primary constructor, so a copied instance has a `null` cause.
     */
    data class Unknown(
        override val message: String,
    ) : Failure {

        override var cause: Throwable? = null
            private set

        /**
         * Creates an unexpected failure that keeps [cause] for diagnosis; see the class KDoc.
         */
        constructor(
            message: String,
            cause: Throwable?,
        ) : this(message) {
            this.cause = cause
        }
    }
}
