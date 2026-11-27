package com.raxdenstudios.platform.core.di

/**
 * Implemented by the app's `Application` to expose the [DIApp] that composes its DI graph, so
 * [com.raxdenstudios.platform.core.initializer.DIInitializer] can start it without knowing the
 * app's classes.
 *
 * ```kotlin
 * class App : Application(), DIAppProvider {
 *     override val diApp: DIApp = AppDIApp()
 * }
 * ```
 */
interface DIAppProvider {

    /**
     * The app's DI graph. App Startup runs before `Application.onCreate`, so it must be ready when
     * the `Application` is constructed: build it in a property initializer or the constructor,
     * never in `onCreate`.
     */
    val diApp: DIApp
}
