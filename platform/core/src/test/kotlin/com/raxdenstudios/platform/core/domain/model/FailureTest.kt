package com.raxdenstudios.platform.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class FailureTest {

    @Test
    fun `Given secondary constructor When creating Unknown Then keeps the same cause instance`() {
        val cause = IllegalStateException("boom")

        val result = Failure.Unknown("message", cause)

        assertThat(result.message).isEqualTo("message")
        assertThat(result.cause).isSameInstanceAs(cause)
    }

    @Test
    fun `Given primary constructor When creating Unknown Then cause is null`() {
        val result = Failure.Unknown("message")

        assertThat(result.cause).isNull()
    }

    @Test
    fun `Given Unknown with different causes When comparing Then they are equal`() {
        val withCause = Failure.Unknown("message", IllegalStateException("boom"))
        val withoutCause = Failure.Unknown("message")

        assertThat(withCause).isEqualTo(withoutCause)
        assertThat(withCause.hashCode()).isEqualTo(withoutCause.hashCode())
    }

    @Test
    fun `Given Unknown with cause When toString Then cause is not included`() {
        val result = Failure.Unknown("message", IllegalStateException("boom"))

        assertThat(result.toString()).isEqualTo(Failure.Unknown("message").toString())
    }

    @Test
    fun `Given Unknown with cause When copy Then cause is dropped`() {
        val result = Failure.Unknown("message", IllegalStateException("boom")).copy(message = "other")

        assertThat(result.message).isEqualTo("other")
        assertThat(result.cause).isNull()
    }

    @Test
    fun `Given other failure subtypes When reading cause Then it is null`() {
        assertThat(Failure.Network.Client("m").cause).isNull()
        assertThat(Failure.Network.Server("m").cause).isNull()
        assertThat(Failure.Network.Connection("m").cause).isNull()
        assertThat(Failure.Network.Unknown("m").cause).isNull()
        assertThat(Failure.Unauthorized("m").cause).isNull()
        assertThat(Failure.ResourceNotFound("m").cause).isNull()
    }

    @Test
    fun `Given custom Failure implementation When reading cause Then it is null`() {
        val failure = object : Failure {
            override val message: String = "custom"
        }

        assertThat(failure.cause).isNull()
    }
}
