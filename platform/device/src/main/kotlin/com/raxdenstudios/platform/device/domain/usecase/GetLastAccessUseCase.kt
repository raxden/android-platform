package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.repository.AppRepository

interface GetLastAccessUseCase : SuspendUseCase<GetLastAccessUseCase.Params, Long> {

    data object Params
}

internal class GetLastAccessUseCaseImpl(
    private val appRepository: AppRepository,
) : GetLastAccessUseCase {

    override suspend fun invoke(
        params: GetLastAccessUseCase.Params,
    ): Answer<Long, Failure> =
        appRepository.getLastAccess()
}
