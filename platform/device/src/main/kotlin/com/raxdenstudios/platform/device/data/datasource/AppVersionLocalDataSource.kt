package com.raxdenstudios.platform.device.data.datasource

import android.content.Context
import com.raxdenstudios.commons.android.util.SDK
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure

/** Reads the version of the app currently installed on the device. */
internal interface AppVersionLocalDataSource {

    /**
     * The installed `versionCode`. Fails when the package info cannot be read, which is reported as a version code
     * of 0.
     */
    suspend fun getInstalledVersionCode(): Answer<Long, Failure>
}

internal class AppVersionLocalDataSourceImpl(
    private val context: Context,
) : AppVersionLocalDataSource {

    override suspend fun getInstalledVersionCode(): Answer<Long, Failure> =
        when (val versionCode = SDK.getVersionCode(context)) {
            UNKNOWN_VERSION_CODE -> Answer.Failure(Failure.Unknown("Installed version code could not be read"))
            else -> Answer.Success(versionCode)
        }

    private companion object {
        const val UNKNOWN_VERSION_CODE = 0L
    }
}
