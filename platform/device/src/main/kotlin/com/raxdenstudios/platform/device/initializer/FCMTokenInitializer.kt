package com.raxdenstudios.platform.device.initializer

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer
import com.raxdenstudios.commons.core.ext.onFailure
import com.raxdenstudios.commons.core.ext.onSuccess
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.platform.core.initializer.DIInitializer
import com.raxdenstudios.platform.device.domain.usecase.GetFCMTokenUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * androidx.startup initializer that logs the device's FCM token at debug level, so a developer can
 * copy it from Logcat to send test notifications. It runs only in debuggable apps; elsewhere
 * [create] returns without touching Koin.
 *
 * The build type is read at runtime from [ApplicationInfo.FLAG_DEBUGGABLE], as
 * [TimberInitializer] does: the library `BuildConfig.DEBUG` is baked into the AAR and is always
 * false for consumers. A failure to get the token is logged at error level and is not fatal.
 *
 * ## Koin
 *
 * [create] resolves [GetFCMTokenUseCase] (bound by `DeviceDILibrary`) and [CoroutineScopeProvider]
 * (bound by platform-core's `CoreDILibrary`) and launches the request on
 * [CoroutineScopeProvider.io]. [dependencies] lists [DIInitializer], so Koin is started first, and
 * [TimberInitializer], so the token is not logged before a tree is planted.
 *
 * ## Setup
 *
 * Opt-in: the library manifest does not declare it. Enable it from the app manifest, inside the
 * androidx.startup provider, in place of the app's own FCM token initializer:
 *
 * ```xml
 * <meta-data
 *     android:name="com.raxdenstudios.platform.device.initializer.FCMTokenInitializer"
 *     android:value="androidx.startup" />
 * ```
 */
class FCMTokenInitializer :
    Initializer<Unit>,
    KoinComponent {

    private val coroutineScopeProvider: CoroutineScopeProvider by inject()
    private val getFCMTokenUseCase: GetFCMTokenUseCase by inject()

    override fun create(
        context: Context,
    ) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return

        coroutineScopeProvider.io.launch {
            getFCMTokenUseCase(GetFCMTokenUseCase.Params)
                .onSuccess { token -> Timber.d("FCM Token: $token") }
                .onFailure { error -> Timber.e("Failed to get FCM token: ${error.message}") }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        DIInitializer::class.java,
        TimberInitializer::class.java,
    )
}
