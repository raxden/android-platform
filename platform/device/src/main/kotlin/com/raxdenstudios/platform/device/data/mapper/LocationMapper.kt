package com.raxdenstudios.platform.device.data.mapper

import android.location.Location as AndroidLocation
import com.raxdenstudios.commons.core.util.DataMapper
import com.raxdenstudios.platform.device.domain.model.Location

interface LocationMapper {
    fun transform(source: AndroidLocation): Location
}

class LocationMapperImpl : DataMapper<AndroidLocation, Location>(), LocationMapper {

    override fun transform(source: AndroidLocation): Location = Location(
        latitude = source.latitude,
        longitude = source.longitude,
    )
}
