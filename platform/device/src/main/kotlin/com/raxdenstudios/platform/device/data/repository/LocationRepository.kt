package com.raxdenstudios.platform.device.data.repository

import android.location.Location
import com.raxdenstudios.platform.device.data.LocationManager
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface LocationRepository {
    suspend fun getLastLocation(): Answer<Location, Failure>
}

internal class LocationRepositoryImpl(
    private val locationManager: LocationManager,
) : LocationRepository {

    override suspend fun getLastLocation(): Answer<Location, Failure> =
        suspendCancellableCoroutine { continuation ->
            locationManager.getLastLocation(
                onSuccess = { continuation.resume(Answer.Success(it)) },
                onFailure = {
                    continuation.resume(
                        Answer.Failure(
                            Failure.Unknown(
                                it.message ?: "Location unavailable"
                            )
                        )
                    )
                },
            )
        }
}
