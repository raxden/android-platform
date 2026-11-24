package com.raxdenstudios.platform.core.domain.ext

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.platform.core.domain.model.Failure
import org.junit.Test

internal class FailureExtensionTest {

    @Test
    fun `Given throwable with message When toUnknownError Then message includes throwable message`() {
        val result = Throwable("boom").toUnknownError("Operation failed")

        assertThat(result).isEqualTo(Failure.Unknown("Operation failed: boom"))
    }

    @Test
    fun `Given throwable without message When toUnknownError Then message uses class name`() {
        val result = IllegalStateException().toUnknownError("Operation failed")

        assertThat(result.message).isEqualTo("Operation failed: IllegalStateException")
    }

    @Test
    fun `Given throwable When toUnknownError Then keeps throwable as cause`() {
        val throwable = IllegalStateException("boom")

        val result = throwable.toUnknownError("Operation failed")

        assertThat(result.cause).isSameInstanceAs(throwable)
    }
}
