package com.raxdenstudios.platform.featureflag.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.featureflag.data.datasource.remote.FeatureFlagRemoteDataSource
import com.raxdenstudios.platform.featureflag.domain.model.FeatureFlag

internal interface FeatureFlagRepository {

    suspend fun fetch(): Answer<Unit, Failure>

    suspend fun isEnabled(
        flag: FeatureFlag,
    ): Answer<Boolean, Failure>
}

internal class FeatureFlagRepositoryImpl(
    private val remoteDataSource: FeatureFlagRemoteDataSource,
) : FeatureFlagRepository {

    override suspend fun fetch(): Answer<Unit, Failure> = remoteDataSource.fetchAndActivate()

    override suspend fun isEnabled(
        flag: FeatureFlag,
    ): Answer<Boolean, Failure> = remoteDataSource.isEnabled(flag)
}
