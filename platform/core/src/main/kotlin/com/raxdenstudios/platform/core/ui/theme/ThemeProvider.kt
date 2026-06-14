package com.raxdenstudios.platform.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

interface ThemeProvider {

    val theme: MutableState<Theme>

    @Composable
    fun Apply(
        content: @Composable () -> Unit,
    )
}

class DefaultThemeProvider : ThemeProvider {

    override val theme: MutableState<Theme> = mutableStateOf(Theme.Device)

    @Composable
    override fun Apply(
        content: @Composable () -> Unit,
    ) {
        AppComposeTheme(
            colorScheme = when (val theme = theme.value) {
                is Theme.App -> when (theme.dark) {
                    true -> darkColorScheme()
                    false -> lightColorScheme()
                }
                Theme.Device -> when (isSystemInDarkTheme()) {
                    true -> darkColorScheme()
                    false -> lightColorScheme()
                }
            },
            content = content
        )
    }
}

sealed class Theme {

    data object Device : Theme()
    data class App(val dark: Boolean) : Theme()
}

