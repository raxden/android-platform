package com.raxdenstudios.platform.core.initializer

import android.content.Context
import androidx.startup.Initializer
import com.raxdenstudios.platform.core.di.DIAppProvider
import org.koin.core.context.GlobalContext

/**
 * androidx.startup initializer that starts the app's DI by calling [DIAppProvider.diApp]
 * `initialize` on the app's `Application`. Library and app initializers that need Koin in
 * [Initializer.create] list it in their [Initializer.dependencies], without knowing the app's
 * own Koin initializer.
 *
 * It does nothing when Koin is already started, and fails with a clear error when the
 * `Application` does not implement [DIAppProvider]. It needs nothing else to start Koin, so
 * [dependencies] is empty: an initializer that logs while it runs must also depend on the
 * Timber initializer of platform-device.
 *
 * "Already started" means Koin's global context is registered. `startKoin` registers it before
 * loading the modules, so a first start that throws after that point is treated as started and is
 * not retried by a later call.
 *
 * ## Coexisting with an app Koin initializer
 *
 * Delete the app's own Koin initializer. If the app keeps it while any enabled initializer depends
 * on [DIInitializer], the order between the two is not defined and the app's `startKoin` can throw
 * `KoinApplicationAlreadyStartedException`. If it must stay, it lists [DIInitializer] in its
 * [Initializer.dependencies] (and then has nothing left to do), or starts Koin only when
 * `GlobalContext.getOrNull() == null`.
 *
 * ## Setup
 *
 * Opt-in: the library manifest does not declare it. Enable it from the app manifest, inside the
 * androidx.startup provider, in place of the app's own Koin initializer. An initializer reached
 * through another one's [Initializer.dependencies] also runs without its own `<meta-data>`, so
 * removing its entry with `tools:node="remove"` only keeps it from running when no enabled
 * initializer depends on it. Initializers that depend on it are opt-in as well. It runs in every
 * process that runs the androidx.startup provider, so multi-process apps start DI in each one.
 *
 * ```xml
 * <meta-data
 *     android:name="com.raxdenstudios.platform.core.initializer.DIInitializer"
 *     android:value="androidx.startup" />
 * ```
 */
class DIInitializer : Initializer<Unit> {

    override fun create(
        context: Context,
    ) {
        if (GlobalContext.getOrNull() != null) return
        val application = context.applicationContext
        val provider = application as? DIAppProvider ?: error(
            "${application::class.java.name} must implement ${DIAppProvider::class.java.name} " +
                "to be started by ${DIInitializer::class.java.name}, or remove the initializer " +
                "(tools:node=\"remove\") and every enabled initializer that depends on it " +
                "from the androidx.startup provider.",
        )
        provider.diApp.initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
