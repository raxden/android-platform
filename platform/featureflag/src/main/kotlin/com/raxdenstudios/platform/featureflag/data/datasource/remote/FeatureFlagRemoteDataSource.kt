package com.raxdenstudios.platform.featureflag.data.datasource.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.data.ext.awaitAnswer
import com.raxdenstudios.platform.core.data.ext.awaitUnitAnswer
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.featureflag.domain.model.FeatureFlag
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The [FeatureFlag] values, backed by Firebase Remote Config.
 *
 * [fetchAndActivate] runs once per process at app start: it sets the in-app defaults from the
 * [FeatureFlag.defaultValue] of every registered flag, applies the minimum fetch interval and fetches
 * and activates the remote values. Each of those three steps has its own 10 second timeout, so a full
 * fetch can take up to about 30 seconds; this never delays reads beyond [FIRST_ACTIVATION_TIMEOUT].
 *
 * A read made while the first activation is still in flight waits for it, at most
 * [FIRST_ACTIVATION_TIMEOUT] counted from that read. Once one read has timed out, or the activation
 * has finished (successfully or not), no later read waits: reads answer the last activated value, or
 * the flag's default when none was ever fetched.
 *
 * Single instance: it holds the first activation gate and the timed-out mark shared by every read.
 */
internal interface FeatureFlagRemoteDataSource {

    /** Sets the in-app defaults, applies the fetch settings and activates the remote values. */
    suspend fun fetchAndActivate(): Answer<Unit, Failure>

    /**
     * Reads [flag] from the activated values. While the first activation is in flight, waits at most
     * two seconds for it; once one read has timed out, no later read waits. Fails only when reading the
     * value throws.
     */
    suspend fun isEnabled(
        flag: FeatureFlag,
    ): Answer<Boolean, Failure>
}

/**
 * @param remoteConfig The Firebase Remote Config instance the values are read from.
 * @param flags The app's flags, registered as in-app defaults.
 * @param minimumFetchInterval How long fetched values stay cached before a new fetch reaches the
 * backend.
 */
internal class FeatureFlagRemoteDataSourceImpl(
    private val remoteConfig: FirebaseRemoteConfig,
    private val flags: List<FeatureFlag>,
    private val minimumFetchInterval: Duration,
) : FeatureFlagRemoteDataSource {

    private val firstActivation = CompletableDeferred<Unit>()

    // Set once a read gave up waiting for the first activation, so later reads do not wait again.
    private val firstActivationTimedOut = AtomicBoolean(false)

    override suspend fun fetchAndActivate(): Answer<Unit, Failure> = try {
        applyDefaults()
            .coFlatMap { applySettings() }
            .coFlatMap { fetchAndActivateRemoteValues() }
    } finally {
        firstActivation.complete(Unit)
    }

    override suspend fun isEnabled(
        flag: FeatureFlag,
    ): Answer<Boolean, Failure> {
        awaitFirstActivation()
        return coRunCatching { readFlag(flag) }
            .thenFailure { error -> error.toUnknownError("Error reading feature flag ${flag.key}") }
    }

    private suspend fun applyDefaults(): Answer<Unit, Failure> = remoteConfig.setDefaultsAsync(buildDefaults())
        .awaitUnitAnswer("Error setting feature flag defaults")

    private suspend fun applySettings(): Answer<Unit, Failure> = remoteConfig.setConfigSettingsAsync(buildSettings())
        .awaitUnitAnswer("Error configuring feature flags")

    private suspend fun fetchAndActivateRemoteValues(): Answer<Unit, Failure> = remoteConfig.fetchAndActivate()
        .awaitAnswer("Error fetching feature flags", FETCH_TIMEOUT)
        .then { }

    // Each read waits at most FIRST_ACTIVATION_TIMEOUT from its own start; once one read has timed out,
    // no later read waits.
    private suspend fun awaitFirstActivation() {
        if (firstActivation.isCompleted || firstActivationTimedOut.get()) return
        withTimeoutOrNull(FIRST_ACTIVATION_TIMEOUT) { firstActivation.await() }
            ?: firstActivationTimedOut.set(true)
    }

    // A STATIC source means the key was never fetched nor has an in-app default: use the flag's default.
    private fun readFlag(
        flag: FeatureFlag,
    ): Boolean {
        val value = remoteConfig.getValue(flag.key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) flag.defaultValue else value.asBoolean()
    }

    private fun buildDefaults(): Map<String, Any> = flags.associate { flag -> flag.key to flag.defaultValue }

    private fun buildSettings(): FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
        .setMinimumFetchIntervalInSeconds(minimumFetchInterval.inWholeSeconds)
        .setFetchTimeoutInSeconds(FETCH_TIMEOUT.inWholeSeconds)
        .build()

    private companion object {
        val FIRST_ACTIVATION_TIMEOUT = 2.seconds
        val FETCH_TIMEOUT = 10.seconds
    }
}
