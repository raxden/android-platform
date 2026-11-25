package com.raxdenstudios.platform.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Holds the [Theme] currently selected and applies it to a Compose hierarchy.
 */
interface ThemeProvider {

    val theme: MutableState<Theme>

    @Composable
    fun Apply(
        content: @Composable () -> Unit,
    )
}

/**
 * Base [ThemeProvider] that applies [AppComposeTheme] with the [ColorScheme] returned by
 * [getColorScheme] for the current [theme].
 *
 * Extend [ColorSchemeThemeProvider] instead when the app only has one light and one dark scheme.
 */
abstract class AbstractThemeProvider : ThemeProvider {

    override val theme: MutableState<Theme> = mutableStateOf(Theme.Device)

    @Composable
    override fun Apply(
        content: @Composable () -> Unit,
    ) {
        AppComposeTheme(
            colorScheme = getColorScheme(theme.value),
            darkTheme = theme.value.isDarkTheme(),
            content = content,
        )
    }

    @Composable
    abstract fun getColorScheme(
        theme: Theme,
    ): ColorScheme
}

/**
 * [ThemeProvider] that picks [darkColorScheme] when the [Theme] resolves to dark (see [isDarkTheme])
 * and [lightColorScheme] otherwise, so apps only pass their schemes.
 */
open class ColorSchemeThemeProvider(
    private val lightColorScheme: ColorScheme,
    private val darkColorScheme: ColorScheme,
) : AbstractThemeProvider() {

    @Composable
    override fun getColorScheme(
        theme: Theme,
    ): ColorScheme = if (theme.isDarkTheme()) darkColorScheme else lightColorScheme
}

/**
 * [ThemeProvider] with the Material 3 baseline light and dark schemes.
 */
class DefaultThemeProvider :
    ColorSchemeThemeProvider(
        lightColorScheme = lightColorScheme(),
        darkColorScheme = darkColorScheme(),
    )

sealed class Theme {

    data object Device : Theme()
    data class App(
        val dark: Boolean,
    ) : Theme()
}

/**
 * Whether this [Theme] renders dark: [Theme.App.dark] for an app choice, the system setting for
 * [Theme.Device].
 */
@Composable
fun Theme.isDarkTheme(): Boolean = when (this) {
    is Theme.App -> dark
    Theme.Device -> isSystemInDarkTheme()
}
