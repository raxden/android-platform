package com.raxdenstudios.platform.network.ext

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.NetworkError
import com.raxdenstudios.platform.core.domain.model.Failure
import org.junit.Test

internal class NetworkErrorExtensionTest {

    @Test
    fun `Given client error When toFailure Then returns Client failure with message`() {
        val error = NetworkError.Client<String>(code = 400, message = "bad request")

        assertThat(error.toFailure()).isEqualTo(Failure.Network.Client("bad request"))
    }

    @Test
    fun `Given server error When toFailure Then returns Server failure with message`() {
        val error = NetworkError.Server<String>(code = 500, message = "server down")

        assertThat(error.toFailure()).isEqualTo(Failure.Network.Server("server down"))
    }

    @Test
    fun `Given network error When toFailure Then returns Connection failure with message`() {
        val error = NetworkError.Network<String>(message = "no connection")

        assertThat(error.toFailure()).isEqualTo(Failure.Network.Connection("no connection"))
    }

    @Test
    fun `Given unknown error When toFailure Then returns Unknown failure with message`() {
        val error = NetworkError.Unknown<String>(message = "unexpected")

        assertThat(error.toFailure()).isEqualTo(Failure.Network.Unknown("unexpected"))
    }
}
