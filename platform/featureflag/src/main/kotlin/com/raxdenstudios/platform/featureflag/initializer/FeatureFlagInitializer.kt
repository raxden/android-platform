package com.raxdenstudios.platform.featureflag.initializer

import android.content.Context
import androidx.startup.Initializer
import com.raxdenstudios.commons.core.ext.onFailure
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.platform.core.initializer.DIInitializer
import com.raxdenstudios.platform.featureflag.domain.FetchFeatureFlagsUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * androidx.startup initializer that fetches and activates the remote feature flags once per
 * process, in the background, by launching [FetchFeatureFlagsUseCase] on
 * [CoroutineScopeProvider.io]. Reads made while the fetch is in flight wait for it only briefly and
 * then use the last activated values or the in-code defaults, so nothing at startup blocks on the
 * network.
 *
 * A failed fetch is only logged as a Timber warning: it does not crash the app, and reads keep
 * answering the last activated values or the defaults.
 *
 * ## Koin
 *
 * [create] resolves [FetchFeatureFlagsUseCase] (bound by `FeatureFlagDILibrary`) and
 * [CoroutineScopeProvider] (bound by platform-core's `CoreDILibrary`) from Koin, so [dependencies]
 * lists [DIInitializer]. The app's `Application` must implement `DIAppProvider` and load both
 * libraries.
 *
 * It does not depend on platform-device's Timber initializer: this module does not depend on
 * platform-device. The warning is written after a network round trip, so in practice the app's tree
 * is planted by then; an app that needs the guarantee lists both initializers from one of its own.
 *
 * ## Setup
 *
 * Opt-in: the library manifest does not declare it. Enable it from the app manifest, inside the
 * androidx.startup provider, in place of the app's own feature flag initializer:
 *
 * ```xml
 * <meta-data
 *     android:name="com.raxdenstudios.platform.featureflag.initializer.FeatureFlagInitializer"
 *     android:value="androidx.startup" />
 * ```
 */
class FeatureFlagInitializer :
    Initializer<Unit>,
    KoinComponent {

    private val coroutineScopeProvider: CoroutineScopeProvider by inject()
    private val fetchFeatureFlagsUseCase: FetchFeatureFlagsUseCase by inject()

    override fun create(
        context: Context,
    ) {
        coroutineScopeProvider.io.launch {
            fetchFeatureFlagsUseCase(FetchFeatureFlagsUseCase.Params)
                .onFailure { error -> Timber.w("Feature flags not fetched: ${error.message}") }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        DIInitializer::class.java,
    )
}
