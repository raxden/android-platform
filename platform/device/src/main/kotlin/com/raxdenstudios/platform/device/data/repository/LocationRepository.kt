package com.raxdenstudios.platform.device.data.repository

import android.Manifest
import androidx.annotation.RequiresPermission
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.datasource.LocationDataSource
import com.raxdenstudios.platform.device.domain.model.Location

interface LocationRepository {

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ],
    )
    suspend fun getLastLocation(): Answer<Location, Failure>
}

internal class LocationRepositoryImpl(private val locationDataSource: LocationDataSource) : LocationRepository {

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ],
    )
    override suspend fun getLastLocation(): Answer<Location, Failure> = locationDataSource.getLastLocation()
}
