package com.raxdenstudios.platform.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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

abstract class AbstractThemeProvider : ThemeProvider {

    override val theme: MutableState<Theme> = mutableStateOf(Theme.Device)

    @Composable
    override fun Apply(
        content: @Composable () -> Unit,
    ) {
        AppComposeTheme(
            colorScheme = getColorScheme(theme.value),
            darkTheme = when (val theme = theme.value) {
                is Theme.App -> theme.dark
                Theme.Device -> isSystemInDarkTheme()
            },
            content = content
        )
    }

    @Composable
    abstract fun getColorScheme(theme: Theme): ColorScheme
}

class DefaultThemeProvider : AbstractThemeProvider() {

//    val dynamicColor: Boolean = false
//    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//        val context = LocalContext.current
//        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//    }
//    darkTheme -> darkScheme
//    else -> lightScheme

    @Composable
    override fun getColorScheme(theme: Theme): ColorScheme = when (theme) {
        is Theme.App -> when (theme.dark) {
            true -> darkColorScheme()
            false -> lightColorScheme()
        }

        Theme.Device -> when (isSystemInDarkTheme()) {
            true -> darkColorScheme()
            false -> lightColorScheme()
        }
    }
}

sealed class Theme {

    data object Device : Theme()
    data class App(val dark: Boolean) : Theme()
}

