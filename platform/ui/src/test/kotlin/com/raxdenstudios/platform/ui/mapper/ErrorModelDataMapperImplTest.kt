package com.raxdenstudios.platform.ui.mapper

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.ui.R
import com.raxdenstudios.platform.ui.model.ErrorModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

internal class ErrorModelDataMapperImplTest {

    private val stringProvider: StringProvider = mockk {
        every { getString(R.string.error_information) } returns "Information"
        every { getString(R.string.error_lost_connection) } returns "No internet connection"
        every { getString(R.string.error_unexpected_error_message) } returns "Unexpected error"
    }
    private val mapper = ErrorModelDataMapperImpl(stringProvider)

    @Test
    fun `toErrorModel keeps message for client failure`() {
        val result = mapper.toErrorModel(Failure.Network.Client("bad request"))

        assertThat(result).isEqualTo(ErrorModel(title = "Information", message = "bad request"))
    }

    @Test
    fun `toErrorModel keeps message for server failure`() {
        val result = mapper.toErrorModel(Failure.Network.Server("internal error"))

        assertThat(result).isEqualTo(ErrorModel(title = "Information", message = "internal error"))
    }

    @Test
    fun `toErrorModel maps connection failure to lost connection message`() {
        val result = mapper.toErrorModel(Failure.Network.Connection("timeout"))

        assertThat(result)
            .isEqualTo(ErrorModel(title = "Information", message = "No internet connection"))
    }

    @Test
    fun `toErrorModel maps unknown failures to generic message`() {
        listOf(
            Failure.Network.Unknown("e"),
            Failure.ResourceNotFound("e"),
            Failure.Unauthorized("e"),
            Failure.Unknown("e"),
        ).forEach { failure ->
            val result = mapper.toErrorModel(failure)

            assertThat(result)
                .isEqualTo(ErrorModel(title = "Information", message = "Unexpected error"))
        }
    }
}
