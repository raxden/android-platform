package com.raxdenstudios.platform.core.di.modules

import androidx.work.WorkManager
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.commons.coroutines.DefaultCoroutineScopeProvider
import com.raxdenstudios.commons.coroutines.DefaultDispatcherProvider
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.platform.core.di.DIModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Generic, app-agnostic Koin definitions every app used to declare on its own.
 *
 * Every binding is an unqualified singleton:
 * - [DispatcherProvider] as [DefaultDispatcherProvider].
 * - [CoroutineScopeProvider] as [DefaultCoroutineScopeProvider], built from the bound
 *   [DispatcherProvider]. Its scopes are app-lifetime, so it must stay a singleton.
 * - [WorkManager] from the application context.
 */
internal class CoreDIModule : DIModule {

    override fun get(): Module = module {
        single<DispatcherProvider> { DefaultDispatcherProvider() }
        single<CoroutineScopeProvider> {
            DefaultCoroutineScopeProvider(
                dispatcherProvider = get(),
            )
        }
        single { WorkManager.getInstance(androidContext()) }
    }
}
