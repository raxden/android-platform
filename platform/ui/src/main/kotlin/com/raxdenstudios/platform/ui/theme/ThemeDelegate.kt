package com.raxdenstudios.platform.ui.theme

import com.raxdenstudios.commons.coroutines.ext.safeLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Source of the [Theme] the user selected, implemented by the app.
 *
 * The app maps its own stored theme to the platform [Theme] here, so its persisted model does not
 * have to change.
 */
fun interface ThemeSource {

    /**
     * Emits the [Theme] the user selected and every later change to it.
     *
     * The flow must neither complete nor throw: either one ends the theme sync for good, and
     * [ThemeDelegate] does not retry. Implementations should drop failed results instead, for
     * example with `mapNotNull` on the use case's `Answer`, as the README shows.
     */
    fun observe(): Flow<Theme>
}

/**
 * Keeps the [ThemeManager] in sync with the theme the user selected.
 */
interface ThemeDelegate {

    /**
     * Collects the selected theme in [scope] and applies every change until [scope] is cancelled.
     *
     * [scope] must be supervised, such as `viewModelScope`. The collection runs under `safeLaunch`,
     * whose exception handler only applies under a `SupervisorJob`; with a plain `Job` parent, a
     * failing [ThemeSource] cancels the whole scope. A [ThemeSource] that throws also ends the theme
     * sync for good, because the delegate does not retry.
     */
    fun observeTheme(
        scope: CoroutineScope,
    )
}

/**
 * [ThemeDelegate] that forwards each distinct [Theme] emitted by [themeSource] to [themeManager].
 */
class ThemeDelegateImpl(
    private val themeManager: ThemeManager,
    private val themeSource: ThemeSource,
) : ThemeDelegate {

    override fun observeTheme(
        scope: CoroutineScope,
    ) {
        scope.safeLaunch {
            themeSource.observe()
                .distinctUntilChanged()
                .collect { theme -> themeManager.update(theme) }
        }
    }
}
