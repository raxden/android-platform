package com.raxdenstudios.platform.featureflag.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.featureflag.di.module.FeatureFlagDataDIModule
import com.raxdenstudios.platform.featureflag.di.module.FeatureFlagDomainDIModule
import com.raxdenstudios.platform.featureflag.domain.model.FeatureFlag
import kotlin.time.Duration

/**
 * DI entry point of the feature flags, backed by Firebase Remote Config. Exposes
 * `IsFeatureFlagEnabledUseCase` and `FetchFeatureFlagsUseCase`; `FetchFeatureFlagsUseCase` must run
 * once at app start.
 *
 * Firebase types stay inside the data layer: callers only see the use cases and [FeatureFlag]. The
 * app must have Firebase configured (`google-services.json` and the Google Services plugin).
 *
 * @param flags Every flag the app declares. Each one is registered as a Remote Config in-app default
 * with its [FeatureFlag.defaultValue]; keys must be unique.
 * @param minimumFetchInterval How long fetched values stay cached before a new fetch reaches the
 * backend. Pass it from the app's own build type: a library `BuildConfig.DEBUG` is always `false`
 * for consumers. Must not be negative. Remote Config takes whole seconds, so any sub-second part is
 * truncated (e.g. `1.5.seconds` becomes 1 second).
 * @throws IllegalArgumentException if [minimumFetchInterval] is negative.
 *
 * ## Setup
 *
 * 1. Declare the flags with an enum implementing [FeatureFlag]:
 *
 * ```kotlin
 * enum class AppFeatureFlag(
 *     override val key: String,
 *     override val defaultValue: Boolean,
 * ) : FeatureFlag {
 *
 *     PODCASTS(key = "podcasts_enabled", defaultValue = false),
 * }
 * ```
 *
 * 2. Register the library next to the other `DILibrary` instances. Debug builds use a zero interval
 * so a console change shows up on the next launch:
 *
 * ```kotlin
 * override fun provideLibs(): List<DILibrary> = listOf(
 *     FeatureFlagDILibrary(
 *         flags = AppFeatureFlag.entries,
 *         minimumFetchInterval = if (BuildConfig.DEBUG) Duration.ZERO else 1.hours,
 *     ),
 *     // ...
 * )
 * ```
 *
 * 3. Fetch once per process at app start by registering
 * [com.raxdenstudios.platform.featureflag.initializer.FeatureFlagInitializer] in the app's
 * androidx.startup provider. It depends on platform-core's `DIInitializer`, so Koin is started
 * first:
 *
 * ```xml
 * <meta-data
 *     android:name="com.raxdenstudios.platform.featureflag.initializer.FeatureFlagInitializer"
 *     android:value="androidx.startup" />
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * val enabled = isFeatureFlagEnabledUseCase(
 *     IsFeatureFlagEnabledUseCase.Params(AppFeatureFlag.PODCASTS),
 * ).getValueOrDefault(AppFeatureFlag.PODCASTS.defaultValue)
 * ```
 *
 * To add a flag, add an entry to the app's enum and create the matching parameter in the Firebase
 * console. No change is needed in this library.
 */
class FeatureFlagDILibrary(
    private val flags: List<FeatureFlag>,
    private val minimumFetchInterval: Duration,
) : DILibrary {

    init {
        require(!minimumFetchInterval.isNegative()) {
            "minimumFetchInterval must not be negative, was $minimumFetchInterval"
        }
    }

    override fun initialize() = Unit

    override fun getModules(): List<DIModule> = listOf(
        FeatureFlagDataDIModule(
            flags = flags,
            minimumFetchInterval = minimumFetchInterval,
        ),
        FeatureFlagDomainDIModule(),
    )
}
