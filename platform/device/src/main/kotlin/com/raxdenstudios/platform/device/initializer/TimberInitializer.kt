package com.raxdenstudios.platform.device.initializer

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer
import com.raxdenstudios.platform.device.logging.FirebaseCrashlyticsTree
import timber.log.Timber

/**
 * androidx.startup initializer that plants the Timber tree, so logs written while the app starts
 * are not lost. Debuggable builds plant [Timber.DebugTree] (Logcat); other builds plant
 * [FirebaseCrashlyticsTree], which forwards logged throwables to Crashlytics.
 *
 * The build type is read at runtime from [ApplicationInfo.FLAG_DEBUGGABLE]: the library
 * `BuildConfig.DEBUG` is baked into the AAR and is always false for consumers. This is a deliberate
 * difference from `BuildConfig.DEBUG`: a release build marked `debuggable=true` plants
 * [Timber.DebugTree], and a debug build with `debuggable=false` plants [FirebaseCrashlyticsTree].
 *
 * ## Setup
 *
 * Opt-in: the library manifest does not declare it. Enable it from the app manifest, inside the
 * androidx.startup provider, or list it in [Initializer.dependencies] of an app initializer that
 * must run after it (for example the app's Koin initializer):
 *
 * ```xml
 * <meta-data
 *     android:name="com.raxdenstudios.platform.device.initializer.TimberInitializer"
 *     android:value="androidx.startup" />
 * ```
 *
 * The app must remove its own Timber initializer: planting from both would log every entry twice.
 * It needs no Koin, so [dependencies] is empty.
 */
class TimberInitializer : Initializer<Unit> {

    override fun create(
        context: Context,
    ) {
        Timber.plant(buildTree(context))
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun buildTree(
        context: Context,
    ): Timber.Tree = when {
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 -> Timber.DebugTree()
        else -> FirebaseCrashlyticsTree()
    }
}
