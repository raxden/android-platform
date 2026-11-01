package com.raxdenstudios.platform.device.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.repository.LocationRepository
import com.raxdenstudios.platform.device.domain.model.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetLastLocationUseCaseImplTest {

    private val locationRepository: LocationRepository = mockk()
    private val useCase = GetLastLocationUseCaseImpl(locationRepository)

    @Test
    fun `invoke returns repository answer`() = runTest {
        val location = Location(latitude = 40.0, longitude = -3.0)
        coEvery { locationRepository.getLastLocation() } returns Answer.Success(location)

        val result = useCase(GetLastLocationUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success(location))
        coVerify { locationRepository.getLastLocation() }
    }

    @Test
    fun `invoke propagates failure`() = runTest {
        val failure = Failure.Unknown("unavailable")
        coEvery { locationRepository.getLastLocation() } returns Answer.Failure(failure)

        val result = useCase(GetLastLocationUseCase.Params)

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
