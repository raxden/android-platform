package com.raxdenstudios.platform.core.di

import com.raxdenstudios.platform.core.di.modules.CoreDIModule

/**
 * DI entry point of platform-core: the generic bindings every app needs, as unqualified
 * singletons:
 * - `DispatcherProvider` (`DefaultDispatcherProvider`).
 * - `CoroutineScopeProvider` (`DefaultCoroutineScopeProvider`, built from the bound
 *   `DispatcherProvider`).
 * - `WorkManager` (`WorkManager.getInstance(androidContext())`).
 *
 * ## Setup
 *
 * List it first in `provideLibs()`. Koin lets the definition loaded last win, so an app override listed
 * after it wins. Apps should still delete their copies (see below):
 *
 * ```kotlin
 * override fun provideLibs(): List<DILibrary> = listOf(
 *     CoreDILibrary(),
 *     UIDILibrary(),
 *     DeviceDILibrary(),
 *     // ...
 * )
 * ```
 *
 * Delete the app's own definitions of these three types (e.g. its `CoroutinesDIModule` and the
 * `WorkManager.getInstance(...)` single). `KoinDIApp` keeps Koin's default `allowOverride = true`,
 * so a copy does not fail at startup: the definition loaded last silently replaces the other one.
 * That hides which one is live and lets the two drift apart.
 *
 * To replace one on purpose (e.g. a test `DispatcherProvider`), bind it in a module loaded after
 * this library, i.e. listed after `CoreDILibrary` in `provideLibs()`.
 */
class CoreDILibrary : DILibrary {

    override fun initialize() {
        // empty block
    }

    override fun getModules(): List<DIModule> = listOf(
        CoreDIModule(),
    )
}
