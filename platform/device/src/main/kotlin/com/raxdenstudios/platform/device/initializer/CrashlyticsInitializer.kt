package com.raxdenstudios.platform.device.initializer

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.startup.Initializer
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * androidx.startup initializer that adds the app version to every Crashlytics report, as the custom
 * keys [KEY_APP_VERSION] (version name) and [KEY_APP_VERSION_CODE] (version code). Both are read at
 * runtime from the app's [PackageInfo], since the library `BuildConfig` describes the AAR, not the
 * app. Whether reports are collected is set per build type by the
 * `firebase_crashlytics_collection_enabled` manifest flag, not here.
 *
 * ## Setup
 *
 * Opt-in: the library manifest does not declare it. Enable it from the app manifest, inside the
 * androidx.startup provider:
 *
 * ```xml
 * <meta-data
 *     android:name="com.raxdenstudios.platform.device.initializer.CrashlyticsInitializer"
 *     android:value="androidx.startup" />
 * ```
 *
 * It needs no Koin, so [dependencies] is empty.
 */
class CrashlyticsInitializer internal constructor(
    private val crashlytics: () -> FirebaseCrashlytics,
) : Initializer<Unit> {

    /** Constructor used by androidx.startup; reads the default [FirebaseCrashlytics] instance. */
    constructor() : this(crashlytics = { FirebaseCrashlytics.getInstance() })

    override fun create(
        context: Context,
    ) {
        val packageInfo = context.packageManager.packageInfoOf(context.packageName)
        crashlytics().apply {
            setCustomKey(KEY_APP_VERSION, packageInfo.versionName.orEmpty())
            setCustomKey(KEY_APP_VERSION_CODE, PackageInfoCompat.getLongVersionCode(packageInfo).toInt())
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun PackageManager.packageInfoOf(
        packageName: String,
    ): PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, 0)
    }

    companion object {

        /** Crashlytics custom key holding the app version name. */
        const val KEY_APP_VERSION = "app_version"

        /** Crashlytics custom key holding the app version code. */
        const val KEY_APP_VERSION_CODE = "app_version_code"
    }
}
