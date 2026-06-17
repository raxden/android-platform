package com.raxdenstudios.platform.device.data.repository

import android.content.Context
import com.raxdenstudios.commons.android.util.SDK
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.device.data.datasource.AppAccessLocalDataSource

interface AppRepository {
    suspend fun getLastAccess(): Answer<Long, Failure>
    suspend fun saveLastAccess(timestamp: Long): Answer<Unit, Failure>
    suspend fun getAppVersion(): Answer<String, Failure>
}

class AppRepositoryImpl(
    private val context: Context,
    private val appAccessLocalDataSource: AppAccessLocalDataSource,
) : AppRepository {

    override suspend fun getLastAccess(): Answer<Long, Failure> =
        appAccessLocalDataSource.getLastAccess()

    override suspend fun saveLastAccess(timestamp: Long): Answer<Unit, Failure> =
        appAccessLocalDataSource.saveLastAccess(timestamp)

    override suspend fun getAppVersion(): Answer<String, Failure> =
        Answer.Success(SDK.getVersionName(context))
}
