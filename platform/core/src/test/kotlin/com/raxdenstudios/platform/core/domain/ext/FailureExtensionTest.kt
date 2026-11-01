package com.raxdenstudios.platform.core.domain.ext

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.platform.core.domain.model.Failure
import org.junit.Test

internal class FailureExtensionTest {

    @Test
    fun `toUnknownError returns failure with message and cause`() {
        val result = Throwable("boom").toUnknownError("Operation failed")

        assertThat(result).isEqualTo(Failure.Unknown("Operation failed: boom"))
    }

    @Test
    fun `toUnknownError appends null when throwable has no message`() {
        val result = Throwable().toUnknownError("Operation failed")

        assertThat(result).isEqualTo(Failure.Unknown("Operation failed: null"))
    }
}
