package com.raxdenstudios.platform.featureflag.domain.model

/**
 * A switch that turns a part of the app on or off remotely, without a release.
 *
 * Each flag is a boolean Remote Config parameter named [key], toggled in the Firebase console. When
 * the parameter has never been fetched (first launch offline, Remote Config unreachable) or does not
 * exist in the console, the flag takes [defaultValue]. Every flag passed to
 * [com.raxdenstudios.platform.featureflag.di.FeatureFlagDILibrary] is registered as an in-app default
 * automatically, so declaring the flag is all the library needs.
 *
 * Apps implement it with their own enum, one entry per flag, each with a unique [key]:
 *
 * ```kotlin
 * enum class AppFeatureFlag(
 *     override val key: String,
 *     override val defaultValue: Boolean,
 * ) : FeatureFlag {
 *
 *     PODCASTS(key = "podcasts_enabled", defaultValue = false),
 *     NEW_FEATURE(key = "new_feature_enabled", defaultValue = false), // new entry
 * }
 * ```
 *
 * ## Adding a flag
 *
 * 1. Add an entry to the app's enum with a unique [key] and the default that keeps the feature off.
 * 2. Create the parameter in the Firebase console with the same key.
 * 3. Read it through [com.raxdenstudios.platform.featureflag.domain.IsFeatureFlagEnabledUseCase]:
 *
 * ```kotlin
 * val enabled = isFeatureFlagEnabledUseCase(
 *     IsFeatureFlagEnabledUseCase.Params(AppFeatureFlag.NEW_FEATURE),
 * ).getValueOrDefault(AppFeatureFlag.NEW_FEATURE.defaultValue)
 * ```
 *
 * @property key The Remote Config parameter name. It must match the key set in the Firebase console.
 * @property defaultValue The value used until a remote value has been activated.
 */
interface FeatureFlag {

    val key: String

    val defaultValue: Boolean
}
