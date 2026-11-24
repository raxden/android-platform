package com.raxdenstudios.platform.device.logging

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.coroutines.ext.defaultCoroutineLogger
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber

internal class TimberCoroutineLoggerTest {

    private val tree = RecordingTree()
    private val previousLogger = defaultCoroutineLogger

    @Before
    fun setUp() {
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uproot(tree)
        defaultCoroutineLogger = previousLogger
    }

    @Test
    fun `Given tag message and throwable When logError Then forwards them to Timber at error level`() {
        val exception = IllegalStateException("boom")

        TimberCoroutineLogger().logError("CoroutineException", "failed", exception)

        val entry = tree.entries.single()
        assertThat(entry.priority).isEqualTo(Log.ERROR)
        assertThat(entry.tag).isEqualTo("CoroutineException")
        assertThat(entry.message).startsWith("failed")
        assertThat(entry.throwable).isSameInstanceAs(exception)
    }

    @Test
    fun `Given null message When logError Then logs the throwable with the given tag`() {
        val exception = IllegalStateException("boom")

        TimberCoroutineLogger().logError("CoroutineException", null, exception)

        val entry = tree.entries.single()
        assertThat(entry.tag).isEqualTo("CoroutineException")
        assertThat(entry.throwable).isSameInstanceAs(exception)
        assertThat(entry.message).contains("IllegalStateException")
    }

    @Test
    fun `Given install When called Then sets default coroutine logger to a TimberCoroutineLogger`() {
        defaultCoroutineLogger = null

        TimberCoroutineLogger.install()

        assertThat(defaultCoroutineLogger).isInstanceOf(TimberCoroutineLogger::class.java)
    }

    @Test
    fun `Given install called twice When called Then replaces the previous logger`() {
        TimberCoroutineLogger.install()
        val first = defaultCoroutineLogger

        TimberCoroutineLogger.install()

        assertThat(defaultCoroutineLogger).isNotSameInstanceAs(first)
    }

    @Test
    fun `Given installed logger When default logger logs error Then reaches Timber`() {
        val exception = IllegalStateException("boom")
        TimberCoroutineLogger.install()

        defaultCoroutineLogger?.logError("CoroutineException", "failed", exception)

        assertThat(tree.entries.single().throwable).isSameInstanceAs(exception)
    }

    private data class Entry(
        val priority: Int,
        val tag: String?,
        val message: String,
        val throwable: Throwable?,
    )

    private class RecordingTree : Timber.Tree() {

        val entries = mutableListOf<Entry>()

        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) {
            entries.add(Entry(priority, tag, message, t))
        }
    }
}
