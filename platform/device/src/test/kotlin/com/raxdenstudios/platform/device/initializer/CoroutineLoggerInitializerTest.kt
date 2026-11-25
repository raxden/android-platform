package com.raxdenstudios.platform.device.initializer

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.coroutines.ext.defaultCoroutineLogger
import com.raxdenstudios.platform.device.logging.TimberCoroutineLogger
import io.mockk.mockk
import org.junit.After
import org.junit.Test

internal class CoroutineLoggerInitializerTest {

    private val previousLogger = defaultCoroutineLogger
    private val context: Context = mockk(relaxed = true)

    @After
    fun tearDown() {
        defaultCoroutineLogger = previousLogger
    }

    @Test
    fun `Given no logger When create Then installs a TimberCoroutineLogger`() {
        defaultCoroutineLogger = null

        CoroutineLoggerInitializer().create(context)

        assertThat(defaultCoroutineLogger).isInstanceOf(TimberCoroutineLogger::class.java)
    }

    @Test
    fun `Given another logger When create Then replaces it with a TimberCoroutineLogger`() {
        defaultCoroutineLogger = mockk(relaxed = true)

        CoroutineLoggerInitializer().create(context)

        assertThat(defaultCoroutineLogger).isInstanceOf(TimberCoroutineLogger::class.java)
    }

    @Test
    fun `When dependencies Then contains only TimberInitializer`() {
        assertThat(CoroutineLoggerInitializer().dependencies()).containsExactly(TimberInitializer::class.java)
    }
}
