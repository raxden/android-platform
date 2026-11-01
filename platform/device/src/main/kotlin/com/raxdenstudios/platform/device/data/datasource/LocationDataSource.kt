package com.raxdenstudios.platform.device.data.datasource

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationServices
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.mapper.LocationMapper
import com.raxdenstudios.platform.device.domain.model.Location
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/** Location access requires an already granted permission. Coroutine cancellation is propagated. */
interface LocationDataSource {

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    suspend fun getLastLocation(): Answer<Location, Failure>

    suspend fun getCountryCode(location: Location): Answer<String, Failure>

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    suspend fun getCountryCode(): Answer<String, Failure>
}

internal class LocationDataSourceImpl(
    context: Context,
    private val dispatcherProvider: DispatcherProvider,
    private val locationMapper: LocationMapper,
) : LocationDataSource {

    private val appContext = context.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    @Suppress("TooGenericExceptionCaught")
    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    override suspend fun getLastLocation(): Answer<Location, Failure> =
        try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    when {
                        task.isCanceled -> continuation.cancel(CancellationException("Location request cancelled"))
                        !task.isSuccessful -> continuation.resume(
                            Answer.Failure(
                                task.exception?.toFailure("Location request failed")
                                    ?: Failure.Unknown("Location request failed")
                            )
                        )

                        else -> continuation.resume(
                            task.result?.let { location ->
                                Answer.Success(locationMapper.transform(location))
                            }
                                ?: Answer.Failure(Failure.ResourceNotFound("Last known location is unavailable"))
                        )
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Answer.Failure(error.toFailure("Location request failed"))
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getCountryCode(location: Location): Answer<String, Failure> {
        val latitude = location.latitude
        val longitude = location.longitude
        return withContext(dispatcherProvider.io) {
            try {
                if (!Geocoder.isPresent()) {
                    return@withContext Answer.Failure(Failure.ResourceNotFound("Geocoding service is unavailable"))
                }
                val geocoder = Geocoder(appContext, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocodeAsync(geocoder, latitude, longitude)
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    countryCodeFrom(addresses)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Answer.Failure(error.toFailure("Geocoding failed"))
            }
        }
    }

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    override suspend fun getCountryCode(): Answer<String, Failure> =
        when (val location = getLastLocation()) {
            is Answer.Success -> getCountryCode(location.value)
            is Answer.Failure -> location
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun geocodeAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ): Answer<String, Failure> = suspendCancellableCoroutine { continuation ->
        // Geocoder has no cancellation API; ignore results after the caller cancels.
        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (continuation.isActive) continuation.resume(countryCodeFrom(addresses))
            }

            override fun onError(errorMessage: String?) {
                if (continuation.isActive) {
                    continuation.resume(
                        Answer.Failure(
                            Failure.Unknown(
                                errorMessage ?: "Geocoding failed"
                            )
                        )
                    )
                }
            }
        })
    }

    private fun countryCodeFrom(addresses: List<Address>?): Answer<String, Failure> {
        val address = addresses?.firstOrNull()
        val countryCode = address?.countryCode?.takeIf { it.isNotBlank() }
        return when {
            address == null ->
                Answer.Failure(Failure.ResourceNotFound("No address found for location"))

            countryCode == null ->
                Answer.Failure(Failure.ResourceNotFound("Country code is unavailable"))

            else -> Answer.Success(countryCode)
        }
    }

    private fun Exception.toFailure(fallbackMessage: String): Failure = when (this) {
        is SecurityException -> Failure.Unauthorized(message ?: fallbackMessage)
        else -> Failure.Unknown(message ?: fallbackMessage)
    }
}
