package com.raxdenstudios.platform.ui.theme

import androidx.compose.runtime.mutableStateOf
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

internal class ThemeManagerImplTest {

    private val themeState = mutableStateOf<Theme>(Theme.Device)
    private val themeProvider: ThemeProvider = mockk {
        every { theme } returns themeState
    }
    private val manager = ThemeManagerImpl(themeProvider)

    @Test
    fun `Given provider with a theme When current Then returns it`() {
        themeState.value = Theme.App(dark = true)

        assertThat(manager.current()).isEqualTo(Theme.App(dark = true))
    }

    @Test
    fun `Given new theme When update Then writes it to the provider`() {
        manager.update(Theme.App(dark = false))

        assertThat(themeState.value).isEqualTo(Theme.App(dark = false))
    }

    @Test
    fun `Given updated theme When current Then returns the updated theme`() {
        manager.update(Theme.App(dark = true))

        assertThat(manager.current()).isEqualTo(Theme.App(dark = true))
    }
}
