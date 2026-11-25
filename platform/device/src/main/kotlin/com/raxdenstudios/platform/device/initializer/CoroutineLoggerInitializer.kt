package com.raxdenstudios.platform.device.initializer

import android.content.Context
import androidx.startup.Initializer
import com.raxdenstudios.platform.device.logging.TimberCoroutineLogger

/**
 * androidx.startup initializer that installs [TimberCoroutineLogger] as the commons
 * `defaultCoroutineLogger`, so the errors caught by the commons coroutine helpers
 * (`defaultExceptionHandler`, `safeLaunch`) are logged through Timber.
 *
 * [dependencies] lists [TimberInitializer], so a tree is planted before any coroutine launched by a
 * later initializer can fail. Enabling this one is enough: App Startup also runs
 * [TimberInitializer].
 *
 * ## Setup
 *
 * Opt-in: the library manifest does not declare it. Enable it from the app manifest, inside the
 * androidx.startup provider, or list it in [Initializer.dependencies] of an app initializer that
 * launches coroutines (for example the app's Koin initializer):
 *
 * ```xml
 * <meta-data
 *     android:name="com.raxdenstudios.platform.device.initializer.CoroutineLoggerInitializer"
 *     android:value="androidx.startup" />
 * ```
 */
class CoroutineLoggerInitializer : Initializer<Unit> {

    override fun create(
        context: Context,
    ) {
        TimberCoroutineLogger.install()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        TimberInitializer::class.java,
    )
}
