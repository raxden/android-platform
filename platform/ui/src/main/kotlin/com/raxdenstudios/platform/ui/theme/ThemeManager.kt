package com.raxdenstudios.platform.ui.theme

interface ThemeManager {

    fun current(): Theme
    fun update(theme: Theme)
}

class ThemeManagerImpl(
    private val themeProvider: ThemeProvider,
) : ThemeManager {

    override fun current(): Theme =
        themeProvider.theme.value

    override fun update(theme: Theme) {
        themeProvider.theme.value = theme
    }
}
