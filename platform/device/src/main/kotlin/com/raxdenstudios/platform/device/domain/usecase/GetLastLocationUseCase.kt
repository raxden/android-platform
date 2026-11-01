package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.repository.LocationRepository
import com.raxdenstudios.platform.device.domain.model.Location

interface GetLastLocationUseCase : SuspendUseCase<GetLastLocationUseCase.Params, Location> {

    data object Params
}

internal class GetLastLocationUseCaseImpl(
    private val locationRepository: LocationRepository,
) : GetLastLocationUseCase {

    override suspend fun invoke(params: GetLastLocationUseCase.Params): Answer<Location, Failure> =
        locationRepository.getLastLocation()
}
