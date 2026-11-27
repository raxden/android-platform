package com.raxdenstudios.platform.device.data.datasource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.onFailure
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.data.ext.awaitAnswer
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.model.AppUpdatePolicy
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [AppUpdatePolicy] published in Firebase Remote Config under [MIN_VERSION_CODE_KEY] and
 * [RECOMMENDED_VERSION_CODE_KEY]. Missing keys read as 0, which disables the threshold.
 */
internal interface AppUpdatePolicyRemoteDataSource {

    /**
     * Refreshes Remote Config (at most once per fetch interval) and returns the active policy. When the refresh
     * fails — e.g. offline — the last activated values are returned instead, so an outage never blocks the app.
     * Fails only when the values themselves cannot be read.
     */
    suspend fun getPolicy(): Answer<AppUpdatePolicy, Failure>

    companion object {

        /** Remote Config key of the minimum `versionCode`; below it the update is mandatory. */
        const val MIN_VERSION_CODE_KEY = "app_update_min_version_code"

        /** Remote Config key of the recommended `versionCode`; below it an optional update is offered. */
        const val RECOMMENDED_VERSION_CODE_KEY = "app_update_recommended_version_code"
    }
}

internal class AppUpdatePolicyRemoteDataSourceImpl(
    private val remoteConfig: FirebaseRemoteConfig,
    private val fetchInterval: Duration,
) : AppUpdatePolicyRemoteDataSource {

    override suspend fun getPolicy(): Answer<AppUpdatePolicy, Failure> {
        refresh()
        return coRunCatching { readPolicy() }
            .thenFailure { error -> error.toUnknownError("Error reading the app update policy") }
    }

    private suspend fun refresh() {
        remoteConfig.fetch(fetchInterval.inWholeSeconds)
            .awaitAnswer(errorMessage = "Error fetching the app update policy", timeout = REFRESH_TIMEOUT)
            .onFailure { failure -> Timber.w(failure.message) }
        remoteConfig.activate()
            .awaitAnswer(errorMessage = "Error activating the app update policy", timeout = REFRESH_TIMEOUT)
            .onFailure { failure -> Timber.w(failure.message) }
    }

    private fun readPolicy() = AppUpdatePolicy(
        minVersionCode = remoteConfig.getLong(AppUpdatePolicyRemoteDataSource.MIN_VERSION_CODE_KEY),
        recommendedVersionCode = remoteConfig.getLong(AppUpdatePolicyRemoteDataSource.RECOMMENDED_VERSION_CODE_KEY),
    )

    private companion object {
        // Keeps app start snappy: past this, the last activated values are used.
        val REFRESH_TIMEOUT = 5.seconds
    }
}
