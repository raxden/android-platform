package com.raxdenstudios.platform.device.data.mapper

import android.location.Location as AndroidLocation
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.platform.device.domain.model.Location
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

internal class LocationMapperImplTest {

    private val mapper = LocationMapperImpl()

    @Test
    fun `transform maps coordinates into domain location`() {
        val androidLocation = mockk<AndroidLocation> {
            every { latitude } returns 40.4168
            every { longitude } returns -3.7038
        }

        val result = mapper.transform(androidLocation)

        assertThat(result).isEqualTo(Location(latitude = 40.4168, longitude = -3.7038))
    }
}
