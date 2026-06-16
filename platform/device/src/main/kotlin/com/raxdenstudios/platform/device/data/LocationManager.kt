package com.raxdenstudios.platform.device.data

import android.Manifest
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.raxdenstudios.commons.android.ActivityHolder
import java.util.Locale

interface LocationManager : DefaultLifecycleObserver {

    /**
     * Attach activity
     *
     * @param activity
     */
    fun attach(activity: ComponentActivity)

    fun getLastLocation(
        onSuccess: (location: Location) -> Unit,
        onFailure: (error: Throwable) -> Unit,
    )

    fun getCountryCode(
        location: Location,
        onSuccess: (countryCode: String) -> Unit,
        onFailure: (error: Throwable) -> Unit,
    )

    fun getCountryCode(
        onSuccess: (countryCode: String) -> Unit,
        onFailure: (error: Throwable) -> Unit,
    )
}

class LocationManagerImpl : LocationManager {

    private val activityHolder: ActivityHolder = ActivityHolder()
    private val activity: ComponentActivity
        get() = activityHolder.activity ?: error("You must call attach() before onCreate()")

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun attach(activity: ComponentActivity) {
        activityHolder.attach(activity)
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun getLastLocation(
        onSuccess: (location: Location) -> Unit,
        onFailure: (error: Throwable) -> Unit,
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onSuccess(location)
            } else {
                onFailure(Throwable("Location is null"))
            }
        }.addOnFailureListener { error ->
            onFailure(error)
        }
    }

    override fun getCountryCode(
        location: Location,
        onSuccess: (countryCode: String) -> Unit,
        onFailure: (error: Throwable) -> Unit,
    ) {
        try {
            val geocoder = Geocoder(activity, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val countryCode = addresses[0].countryCode
                        if (countryCode != null) {
                            onSuccess(countryCode)
                        } else {
                            onFailure(Throwable("Country code is null"))
                        }
                    } else {
                        onFailure(Throwable("No address found for location"))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                if (!addresses.isNullOrEmpty()) {
                    val countryCode = addresses[0].countryCode
                    if (countryCode != null) {
                        onSuccess(countryCode)
                    } else {
                        onFailure(Throwable("Country code is null"))
                    }
                } else {
                    onFailure(Throwable("No address found for location"))
                }
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    override fun getCountryCode(
        onSuccess: (countryCode: String) -> Unit,
        onFailure: (error: Throwable) -> Unit,
    ) {
        getLastLocation(
            onSuccess = { location ->
                getCountryCode(
                    location = location,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                )
            },
            onFailure = onFailure,
        )
    }
}
