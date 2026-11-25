package com.raxdenstudios.platform.ui.theme

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ThemeDelegateImplTest {

    private val themeManager: ThemeManager = mockk(relaxed = true)

    @Test
    fun `Given source with themes When observeTheme Then every theme is applied in order`() = runTest {
        val delegate = delegateOf(flowOf(Theme.Device, Theme.App(dark = true), Theme.App(dark = false)))

        delegate.observeTheme(backgroundScope)
        runCurrent()

        verifyOrder {
            themeManager.update(Theme.Device)
            themeManager.update(Theme.App(dark = true))
            themeManager.update(Theme.App(dark = false))
        }
    }

    @Test
    fun `Given identical consecutive themes When observeTheme Then collapses them`() = runTest {
        val delegate = delegateOf(
            flowOf(Theme.App(dark = true), Theme.App(dark = true), Theme.Device, Theme.Device, Theme.App(dark = true)),
        )

        delegate.observeTheme(backgroundScope)
        runCurrent()

        verify(exactly = 2) { themeManager.update(Theme.App(dark = true)) }
        verify(exactly = 1) { themeManager.update(Theme.Device) }
    }

    @Test
    fun `Given source that throws When observeTheme Then does not crash and keeps earlier updates`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val delegate = delegateOf(
            flow {
                emit(Theme.Device)
                error("boom")
            },
        )

        delegate.observeTheme(scope)
        runCurrent()

        verify(exactly = 1) { themeManager.update(Theme.Device) }
        assertTrue(scope.isActive)
        scope.cancel()
    }

    @Test
    fun `Given scope cancelled When source emits Then no further updates are applied`() = runTest {
        val source = MutableSharedFlow<Theme>()
        val job = Job()
        val scope = CoroutineScope(job + StandardTestDispatcher(testScheduler))
        val delegate = delegateOf(source)
        delegate.observeTheme(scope)
        runCurrent()
        source.emit(Theme.Device)
        runCurrent()

        job.cancel()
        runCurrent()
        source.emit(Theme.App(dark = true))
        runCurrent()

        verify(exactly = 1) { themeManager.update(Theme.Device) }
        verify(exactly = 0) { themeManager.update(Theme.App(dark = true)) }
    }

    private fun delegateOf(
        source: Flow<Theme>,
    ) = ThemeDelegateImpl(
        themeManager = themeManager,
        themeSource = ThemeSource { source },
    )
}
