package com.raxdenstudios.platform.device.data.repository

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.datasource.LocationDataSource
import com.raxdenstudios.platform.device.domain.model.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class LocationRepositoryImplTest {

    private val locationDataSource: LocationDataSource = mockk()
    private val repository = LocationRepositoryImpl(locationDataSource)

    @Test
    fun `getLastLocation returns datasource answer`() = runTest {
        val location = Location(latitude = 40.0, longitude = -3.0)
        coEvery { locationDataSource.getLastLocation() } returns Answer.Success(location)

        val result = repository.getLastLocation()

        assertThat(result).isEqualTo(Answer.Success(location))
        coVerify { locationDataSource.getLastLocation() }
    }

    @Test
    fun `getLastLocation propagates datasource failure`() = runTest {
        val failure = Failure.Unknown("Location unavailable")
        coEvery { locationDataSource.getLastLocation() } returns Answer.Failure(failure)

        val result = repository.getLastLocation()

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
