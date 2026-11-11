package com.raxdenstudios.platform.device.data.mapper

import com.raxdenstudios.commons.core.util.DataMapper
import com.raxdenstudios.platform.device.domain.model.Location
import android.location.Location as AndroidLocation

interface LocationMapper {
    fun transform(source: AndroidLocation): Location
}

class LocationMapperImpl :
    DataMapper<AndroidLocation, Location>(),
    LocationMapper {

    override fun transform(source: AndroidLocation): Location = Location(
        latitude = source.latitude,
        longitude = source.longitude,
    )
}
