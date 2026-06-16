package com.raxdenstudios.platform.device.domain.usecase

import android.location.Location
import com.raxdenstudios.platform.device.data.repository.LocationRepository
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer

interface GetLastLocationUseCase : SuspendUseCase<GetLastLocationUseCase.Params, Location> {

    data object Params
}

internal class GetLastLocationUseCaseImpl(
    private val locationRepository: LocationRepository,
) : GetLastLocationUseCase {

    override suspend fun invoke(params: GetLastLocationUseCase.Params): Answer<Location, Failure> =
        locationRepository.getLastLocation()
}
