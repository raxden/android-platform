package com.raxdenstudios.platform.core.domain.model

interface Failure {

    val message: String

    sealed interface Network : Failure {

        data class Client(
            override val message: String,
        ) : Network

        data class Server(
            override val message: String,
        ) : Network

        data class Connection(
            override val message: String,
        ) : Network

        data class Unknown(
            override val message: String,
        ) : Network
    }

    data class Unauthorized(
        override val message: String,
    ) : Failure

    data class ResourceNotFound(
        override val message: String,
    ) : Failure

    data class Unknown(
        override val message: String,
    ) : Failure
}
