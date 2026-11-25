package com.raxdenstudios.platform.device.initializer

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.startup.Initializer
import com.raxdenstudios.commons.android.ext.registerActivityLifecycleCallbacksOnApplication
import com.raxdenstudios.commons.android.util.ActivityLifecycleCallbacksAdapter
import com.raxdenstudios.commons.permissions.PermissionsManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * androidx.startup initializer that attaches every created [ComponentActivity] to the commons
 * [PermissionsManager] bound by `DeviceDILibrary`, so it can register its permission launchers
 * before the activity is STARTED. Every activity is attached; there is no filter, because a screen
 * that never asks for a permission costs nothing.
 *
 * ## Koin
 *
 * [create] only registers the activity lifecycle callbacks; [PermissionsManager] is resolved from
 * Koin the first time an activity is created, which happens after every initializer and
 * `Application.onCreate` ran. So [dependencies] is empty and this works whoever starts Koin, as
 * long as the app starts it (with `DeviceDILibrary` loaded) in its own initializer or in
 * `Application.onCreate`, before the first activity.
 *
 * ## Setup
 *
 * Opt-in: the library manifest does not declare it. Enable it from the app manifest, inside the
 * androidx.startup provider:
 *
 * ```xml
 * <meta-data
 *     android:name="com.raxdenstudios.platform.device.initializer.PermissionsInitializer"
 *     android:value="androidx.startup" />
 * ```
 *
 * Attach app-specific permission helpers (for example a notification permission launcher) from an
 * initializer of the app; App Startup gives no order between it and this one.
 */
class PermissionsInitializer :
    Initializer<Unit>,
    KoinComponent {

    private val permissionsManager: PermissionsManager by inject()

    override fun create(
        context: Context,
    ) {
        context.registerActivityLifecycleCallbacksOnApplication(PermissionsCallbacks())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private inner class PermissionsCallbacks : ActivityLifecycleCallbacksAdapter() {

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) {
            if (activity !is ComponentActivity) return
            permissionsManager.attach(activity)
        }
    }
}
