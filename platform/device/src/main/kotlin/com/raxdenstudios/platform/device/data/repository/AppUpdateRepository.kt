package com.raxdenstudios.platform.device.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.datasource.AppUpdatePolicyRemoteDataSource
import com.raxdenstudios.platform.device.data.datasource.AppVersionLocalDataSource
import com.raxdenstudios.platform.device.domain.model.AppUpdatePolicy

/** Installed version and remote update policy, the inputs of the update requirement. */
internal interface AppUpdateRepository {

    /** The installed `versionCode`, read from the device; fails when it cannot be read. */
    suspend fun getInstalledVersionCode(): Answer<Long, Failure>

    /** The active remote policy; refreshes Remote Config first, subject to its fetch interval. */
    suspend fun getPolicy(): Answer<AppUpdatePolicy, Failure>
}

internal class AppUpdateRepositoryImpl(
    private val versionLocalDataSource: AppVersionLocalDataSource,
    private val policyRemoteDataSource: AppUpdatePolicyRemoteDataSource,
) : AppUpdateRepository {

    override suspend fun getInstalledVersionCode(): Answer<Long, Failure> =
        versionLocalDataSource.getInstalledVersionCode()

    override suspend fun getPolicy(): Answer<AppUpdatePolicy, Failure> = policyRemoteDataSource.getPolicy()
}
