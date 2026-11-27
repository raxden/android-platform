package com.raxdenstudios.platform.core.initializer

import android.app.Application
import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.platform.core.di.DIApp
import com.raxdenstudios.platform.core.di.DIAppProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

internal class DIInitializerTest {

    private val diApp: DIApp = mockk(relaxed = true)
    private val context: Context = mockk()
    private val initializer = DIInitializer()

    @Before
    fun setUp() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Given koin already started When create Then diApp is not initialized`() {
        startKoin { }
        stubApplication()

        initializer.create(context)

        verify(exactly = 0) { diApp.initialize(any()) }
    }

    @Test
    fun `Given koin not started and provider application When create Then diApp is initialized once`() {
        stubApplication()

        initializer.create(context)

        verify(exactly = 1) { diApp.initialize(context) }
    }

    @Test
    fun `Given application not implementing DIAppProvider When create Then throws IllegalStateException`() {
        every { context.applicationContext } returns mockk<Application>()

        val exception = assertThrows(IllegalStateException::class.java) {
            initializer.create(context)
        }

        assertThat(exception).hasMessageThat().contains("DIAppProvider")
    }

    @Test
    fun `Given initializer When dependencies Then is empty`() {
        assertThat(initializer.dependencies()).isEmpty()
    }

    private fun stubApplication() {
        val application = mockk<Application>(moreInterfaces = arrayOf(DIAppProvider::class))
        every { (application as DIAppProvider).diApp } returns diApp
        every { context.applicationContext } returns application
    }
}
