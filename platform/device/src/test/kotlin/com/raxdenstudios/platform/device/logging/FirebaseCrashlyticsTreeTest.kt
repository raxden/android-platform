package com.raxdenstudios.platform.device.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal class FirebaseCrashlyticsTreeTest {

    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)
    private val tree = FirebaseCrashlyticsTree { crashlytics }

    @Test
    fun `Given verbose with throwable When logged Then does not record`() {
        tree.v(IllegalStateException("boom"), "verbose")

        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Given warn and assert with throwable When logged Then records exceptions`() {
        val warning = IllegalStateException("warn")
        val failure = IllegalArgumentException("wtf")

        tree.w(warning, "warn")
        tree.wtf(failure, "wtf")

        verify(exactly = 1) { crashlytics.recordException(warning) }
        verify(exactly = 1) { crashlytics.recordException(failure) }
    }

    @Test
    fun `Given error with throwable When logged Then records exception`() {
        val exception = IllegalStateException("boom")

        tree.e(exception, "something failed")

        verify(exactly = 1) { crashlytics.recordException(exception) }
    }

    @Test
    fun `Given info with throwable When logged Then records exception`() {
        val exception = IOException("io")

        tree.i(exception, "info")

        verify(exactly = 1) { crashlytics.recordException(exception) }
    }

    @Test
    fun `Given debug with throwable When logged Then does not record`() {
        tree.d(IllegalStateException("boom"), "debug")

        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Given error without throwable When logged Then does not record`() {
        tree.e("only a message")

        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Given connectivity exception When logged Then does not record`() {
        listOf(
            UnknownHostException("host"),
            ConnectException("connect"),
            SocketTimeoutException("timeout"),
        ).forEach { exception -> tree.e(exception, "network failure") }

        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Given connectivity exception as cause When logged Then does not record`() {
        val exception = IllegalStateException("wrapped", RuntimeException("mid", UnknownHostException("host")))

        tree.e(exception, "wrapped failure")

        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Given message mentioning connectivity exception When logged Then does not record`() {
        tree.e(IllegalStateException("boom"), "failed with ${SocketTimeoutException::class.java.name}")

        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Given unrelated exception with unrelated cause When logged Then records exception`() {
        val exception = IllegalStateException("outer", IOException("inner"))

        tree.e(exception, "unrelated")

        verify(exactly = 1) { crashlytics.recordException(exception) }
    }

    @Test
    fun `Given self referencing cause When logged Then terminates and records exception`() {
        val exception = object : RuntimeException("loop") {
            override val cause: Throwable get() = this
        }

        tree.e(exception, "loop")

        verify(exactly = 1) { crashlytics.recordException(exception) }
    }
}
